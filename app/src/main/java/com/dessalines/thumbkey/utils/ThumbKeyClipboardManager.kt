package com.dessalines.thumbkey.utils

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.text.Spanned
import android.util.Log
import com.dessalines.thumbkey.db.CLIP_ITEM_TYPE_IMAGE
import com.dessalines.thumbkey.db.CLIP_ITEM_TYPE_RICH
import com.dessalines.thumbkey.db.CLIP_ITEM_TYPE_TEXT
import com.dessalines.thumbkey.db.ClipboardItem
import com.dessalines.thumbkey.db.ClipboardRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TEXT_MIME_PREFIX = "text/"
private const val IMAGE_MIME_PREFIX = "image/"
private const val MAX_CLIP_TEXT_LENGTH = 5000
private const val SENSITIVE_EXTRA = "android.content.extra.IS_SENSITIVE"

class ThumbKeyClipboardManager(
    private val context: Context,
    private val clipboardRepository: ClipboardRepository,
) {
    private val systemClipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isListening = false
    private var lastClipText: String? = null

// Used to manage data with a non-text MIME type, when private clipboard is enabled
// When copying data with a MIME type different than a text, e.g. a picture, it is not added to the history, as it's not a text. With standard paste it's not an issue as the paste will still paste it.
// However if we paste from the internal clipboard, it will paste the latest string in the history, and not the picture that was only in the system clipboard.
    private var wasLastCopyOperationDoneViaSystem: Boolean = true

    private fun addToClipboardRepo(text: String) {
        addRichClipToRepo(text = text)
    }

    private fun addRichClipToRepo(
        text: String,
        htmlText: String? = null,
        itemType: Int = CLIP_ITEM_TYPE_TEXT,
        uri: String? = null,
        sensitive: Boolean = false,
        timestamp: Long = System.currentTimeMillis(),
    ) {
        if (itemType != CLIP_ITEM_TYPE_IMAGE && text.isBlank()) return
        if (text == lastClipText && uri == null) return
        if (itemType != CLIP_ITEM_TYPE_IMAGE) {
            lastClipText = text
        }
        Log.d(TAG, "Adding clipboard item: type=$itemType sensitive=$sensitive uri=$uri")
        scope.launch {
            clipboardRepository.addItem(
                text = text,
                htmlText = htmlText,
                itemType = itemType,
                uri = uri,
                sensitive = sensitive,
                timestamp = timestamp,
            )
        }
    }

    private fun isSensitiveClip(description: ClipDescription): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                if (description.extras?.getBoolean(SENSITIVE_EXTRA) == true) {
                    return true
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to read sensitive extra", e)
            }
        }
        return false
    }

    private val clipboardListener =
        ClipboardManager.OnPrimaryClipChangedListener {
            val clip = systemClipboardManager.primaryClip
            if (clip == null || clip.itemCount == 0) return@OnPrimaryClipChangedListener

            val description = clip.description
            val timestamp =
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val ts = description.timestamp
                        if (ts > 0) ts else System.currentTimeMillis()
                    } else {
                        System.currentTimeMillis()
                    }
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }
            val sensitive = isSensitiveClip(description)
            val hasTextMime =
                (0 until description.mimeTypeCount).any { index ->
                    val mimeType = description.getMimeType(index)
                    mimeType?.startsWith(TEXT_MIME_PREFIX) == true
                }
            val imageMime =
                (0 until description.mimeTypeCount).firstNotNullOfOrNull { index ->
                    val mimeType = description.getMimeType(index)
                    if (mimeType?.startsWith(IMAGE_MIME_PREFIX) == true) mimeType else null
                }

            if (hasTextMime) {
                val item = clip.getItemAt(0)
                val rawText =
                    try {
                        item.coerceToText(context)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to coerce clipboard content to text", e)
                        null
                    }
                val htmlText =
                    try {
                        item.htmlText
                    } catch (e: Exception) {
                        null
                    }
                val text = rawText?.toString()?.take(MAX_CLIP_TEXT_LENGTH)
                if (!text.isNullOrBlank()) {
                    val itemType =
                        if (!htmlText.isNullOrBlank() || rawText is Spanned) {
                            CLIP_ITEM_TYPE_RICH
                        } else {
                            CLIP_ITEM_TYPE_TEXT
                        }
                    addRichClipToRepo(
                        text = text,
                        htmlText = htmlText?.take(MAX_CLIP_TEXT_LENGTH),
                        itemType = itemType,
                        sensitive = sensitive,
                        timestamp = timestamp,
                    )
                }
            } else if (imageMime != null) {
                // Image-only clip: copy into internal storage (Gboard fmn.b parity).
                try {
                    val sourceUri = clip.getItemAt(0).uri
                    // Ignore our own FileProvider shares (system-paste fallback) to avoid loops.
                    if (sourceUri != null && sourceUri.authority == "${context.packageName}.fileprovider") {
                        return@OnPrimaryClipChangedListener
                    }
                    if (sourceUri != null && sourceUri.toString().startsWith("content://")) {
                        val internalPath =
                            saveClipboardImageToInternal(context, sourceUri, timestamp, imageMime)
                        if (internalPath != null) {
                            addRichClipToRepo(
                                text = "",
                                itemType = CLIP_ITEM_TYPE_IMAGE,
                                uri = internalPath,
                                sensitive = sensitive,
                                timestamp = timestamp,
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to ingest clipboard image", e)
                }
            }

            // Non-text clips are still system copies: pasting them must go through the
            // system clipboard, not the internal text history.
            wasLastCopyOperationDoneViaSystem = true
        }

    fun startListening() {
        if (!isListening) {
            systemClipboardManager.addPrimaryClipChangedListener(clipboardListener)
            isListening = true
        }
    }

    fun stopListening() {
        if (isListening) {
            systemClipboardManager.removePrimaryClipChangedListener(clipboardListener)
            isListening = false
        }
    }

    fun clearExpired() {
        scope.launch {
            clipboardRepository.clearExpired()
            sweepOrphanImages()
        }
    }

    fun sweepOrphanImages() {
        scope.launch {
            try {
                val referenced = clipboardRepository.getAllUris().toSet()
                val dir = resolveClipboardImageDir(context)
                dir.listFiles()?.forEach { file ->
                    if (file.absolutePath !in referenced) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to sweep orphan clipboard images", e)
            }
        }
    }

    fun addPrivateClip(text: String) {
        addRichClipToRepo(text = text)
        wasLastCopyOperationDoneViaSystem = false
    }

    fun wasLastCopyOperationDoneViaSystem(): Boolean = wasLastCopyOperationDoneViaSystem

    fun getLastClip(): String? = lastClipText

    suspend fun getLastClipItem(): ClipboardItem? =
        withContext(Dispatchers.IO) {
            clipboardRepository.getLatest()
        }
}
