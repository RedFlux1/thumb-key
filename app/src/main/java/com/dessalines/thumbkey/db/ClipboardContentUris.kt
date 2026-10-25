package com.dessalines.thumbkey.db

import android.content.ContentUris
import android.content.Context
import android.net.Uri

const val CLIPS_PATH = "clips"
const val CLIP_DIR_CODE = 2
const val CLIP_ITEM_CODE = 1
const val MIME_CLIP_DIR = "vnd.android.cursor.dir/clips"
const val MIME_CLIP_ITEM = "vnd.android.cursor.item/clips"

fun clipboardAuthority(context: Context): String = "${context.packageName}.clipboard_content"

fun buildClipsUri(context: Context): Uri =
    Uri
        .Builder()
        .scheme("content")
        .authority(clipboardAuthority(context))
        .appendPath(CLIPS_PATH)
        .build()

fun buildClipUri(
    context: Context,
    id: Long,
): Uri = ContentUris.withAppendedId(buildClipsUri(context), id)
