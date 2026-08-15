package com.dessalines.thumbkey.utils

import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import com.dessalines.thumbkey.db.ClipboardRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TEXT_MIME_PREFIX = "text/"

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
        if (text.isBlank() || text == lastClipText) return@addToClipboardRepo
        lastClipText = text
        Log.d(TAG, "Adding clipboard item: $text")
        scope.launch {
            clipboardRepository.addItem(text)
        }
    }

    private val clipboardListener =
        ClipboardManager.OnPrimaryClipChangedListener {
            val clip = systemClipboardManager.primaryClip
            if (clip == null || clip.itemCount == 0) return@OnPrimaryClipChangedListener

            // Only text clips belong in history. Non-text clips (e.g. images) cannot be
            // stored, and coercing their URIs to text can throw when the content provider
            // denies access or the URI cannot be resolved (crashing the keyboard).
            val description = clip.description
            val hasTextMime =
                (0 until description.mimeTypeCount).any { index ->
                    val mimeType = description.getMimeType(index)
                    mimeType?.startsWith(TEXT_MIME_PREFIX) == true
                }

            if (hasTextMime) {
                val text =
                    try {
                        clip.getItemAt(0).coerceToText(context)?.toString()
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to coerce clipboard content to text", e)
                        null
                    }
                if (!text.isNullOrBlank()) {
                    addToClipboardRepo(text)
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
        }
    }

    fun addPrivateClip(text: String) {
        addToClipboardRepo(text)
        wasLastCopyOperationDoneViaSystem = false
    }

    fun wasLastCopyOperationDoneViaSystem(): Boolean = wasLastCopyOperationDoneViaSystem

    fun getLastClip(): String? = lastClipText
}
