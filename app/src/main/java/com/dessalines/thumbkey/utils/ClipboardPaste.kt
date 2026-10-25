package com.dessalines.thumbkey.utils

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.inputmethod.InputConnection
import android.widget.Toast
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import com.dessalines.thumbkey.IMEService
import com.dessalines.thumbkey.R
import com.dessalines.thumbkey.db.CLIP_ITEM_TYPE_IMAGE
import com.dessalines.thumbkey.db.ClipboardItem
import java.io.File

fun pasteClipboardItem(
    ime: IMEService,
    item: ClipboardItem,
    stayInClipboard: Boolean = false,
    onReturnToKeyboard: (() -> Unit)? = null,
) {
    val ic = ime.currentInputConnection
    if (item.itemType == CLIP_ITEM_TYPE_IMAGE && !item.uri.isNullOrBlank()) {
        if (!commitClipboardImage(ime, ic, item)) {
            Toast
                .makeText(
                    ime,
                    ime.getString(R.string.clipboard_unsupported_image_paste),
                    Toast.LENGTH_SHORT,
                ).show()
        }
        if (!stayInClipboard) onReturnToKeyboard?.invoke()
        return
    }
    ic?.commitText(item.text, 1)
    if (!stayInClipboard) onReturnToKeyboard?.invoke()
}

private fun commitClipboardImage(
    ime: IMEService,
    ic: InputConnection?,
    item: ClipboardItem,
): Boolean {
    if (ic == null) return false
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return false
    val path = item.uri ?: return false
    return try {
        val file = File(path)
        if (!file.exists()) return false
        val mime =
            when (file.extension.lowercase()) {
                "png" -> "image/png"
                "webp" -> "image/webp"
                "gif" -> "image/gif"
                else -> "image/jpeg"
            }
        val editorInfo = ime.currentInputEditorInfo ?: return false
        val supported =
            editorInfo.contentMimeTypes?.any {
                ClipDescription.compareMimeTypes(it, mime) || it == "*/*"
            } ?: false
        if (!supported) {
            Log.d(TAG, "Editor does not advertise $mime; trying system paste fallback")
            return fallbackSystemImagePaste(ime, ic, file, mime)
        }
        // Expose via internal file uri; commitContent grants temporary read permission.
        val contentUri =
            androidx.core.content.FileProvider.getUriForFile(
                ime,
                "${ime.packageName}.fileprovider",
                file,
            )
        val info =
            InputContentInfoCompat(
                contentUri,
                ClipDescription("", arrayOf(mime)),
                null,
            )
        InputConnectionCompat.commitContent(ic, editorInfo, info, 0, null)
    } catch (e: Exception) {
        Log.w(TAG, "Failed to commit clipboard image", e)
        false
    }
}

private fun fallbackSystemImagePaste(
    ime: IMEService,
    ic: InputConnection?,
    file: File,
    mime: String,
): Boolean {
    if (ic == null) return false
    return try {
        val contentUri =
            androidx.core.content.FileProvider.getUriForFile(
                ime,
                "${ime.packageName}.fileprovider",
                file,
            )
        val cm = ime.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newUri(ime.contentResolver, "image", contentUri))
        ic.performContextMenuAction(android.R.id.paste)
    } catch (e: Exception) {
        Log.w(TAG, "System image paste fallback failed", e)
        false
    }
}

fun supportsClipboardImagePaste(ime: IMEService): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return false
    val mimes = ime.currentInputEditorInfo?.contentMimeTypes ?: return false
    return mimes.any { it.startsWith("image/") || it == "*/*" }
}
