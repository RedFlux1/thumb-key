package com.dessalines.thumbkey.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File

private const val CLIPBOARD_IMAGE_DIR = "clipboard_images"

fun resolveClipboardImageDir(context: Context): File {
    val dir = File(context.filesDir, CLIPBOARD_IMAGE_DIR)
    if (!dir.exists()) {
        dir.mkdirs()
    }
    return dir
}

fun saveClipboardImageToInternal(
    context: Context,
    sourceUri: Uri,
    timestamp: Long,
    mimeType: String?,
): String? {
    return try {
        val extension =
            when {
                mimeType?.contains("png", ignoreCase = true) == true -> "png"
                mimeType?.contains("webp", ignoreCase = true) == true -> "webp"
                mimeType?.contains("gif", ignoreCase = true) == true -> "gif"
                else -> "jpg"
            }
        val dir = resolveClipboardImageDir(context)
        val dest = File(dir, "clip_$timestamp.$extension")
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            dest.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: return null
        dest.absolutePath
    } catch (e: Exception) {
        Log.w(TAG, "Failed to save clipboard image", e)
        null
    }
}

fun deleteClipboardImageFile(path: String?) {
    if (path.isNullOrBlank()) return
    try {
        val file = File(path)
        if (file.exists() && file.parent?.endsWith(CLIPBOARD_IMAGE_DIR) == true) {
            file.delete()
        }
    } catch (e: Exception) {
        Log.w(TAG, "Failed to delete clipboard image file", e)
    }
}
