package com.dessalines.thumbkey.db

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

private val CLIP_COLUMNS =
    arrayOf(
        "_id",
        "text",
        "html_text",
        "item_type",
        "entity_type",
        "timestamp",
        "sensitive",
        "uri",
        "group_id",
        "is_pinned",
    )

class ThumbKeyClipboardProvider : ContentProvider() {
    private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH)

    private fun ensureMatcher(authority: String) {
        // UriMatcher has no contains check; re-adding is idempotent for our codes.
        uriMatcher.addURI(authority, "$CLIPS_PATH/#", CLIP_ITEM_CODE)
        uriMatcher.addURI(authority, CLIPS_PATH, CLIP_DIR_CODE)
    }

    private fun dao(): ClipboardItemDao {
        val ctx = context ?: error("No context")
        return ClipboardDB.getDatabase(ctx).clipboardItemDao()
    }

    private fun itemToRow(item: ClipboardItem): Array<Any?> =
        arrayOf(
            item.id,
            item.text,
            item.htmlText,
            item.itemType,
            item.entityType,
            item.timestamp,
            if (item.sensitive) 1 else 0,
            item.uri,
            item.groupId,
            if (item.isPinned) 1 else 0,
        )

    override fun onCreate(): Boolean {
        val authority = context?.let { clipboardAuthority(it) } ?: return false
        ensureMatcher(authority)
        return true
    }

    override fun getType(uri: Uri): String? {
        val authority = context?.let { clipboardAuthority(it) } ?: return null
        ensureMatcher(authority)
        return when (uriMatcher.match(uri)) {
            CLIP_ITEM_CODE -> MIME_CLIP_ITEM
            CLIP_DIR_CODE -> MIME_CLIP_DIR
            else -> null
        }
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?,
    ): Cursor? {
        val authority = context?.let { clipboardAuthority(it) } ?: return null
        ensureMatcher(authority)
        if (uriMatcher.match(uri) != CLIP_DIR_CODE) return null
        val items: List<ClipboardItem> =
            runBlocking(Dispatchers.IO) {
                dao().getAllSync()
            }
        val cursor = MatrixCursor(CLIP_COLUMNS)
        items.forEach { cursor.addRow(itemToRow(it)) }
        return cursor
    }

    override fun insert(
        uri: Uri,
        values: ContentValues?,
    ): Uri? {
        val ctx = context ?: return uri
        ensureMatcher(clipboardAuthority(ctx))
        val code = uriMatcher.match(uri)
        if (code != CLIP_ITEM_CODE && code != CLIP_DIR_CODE) return uri
        if (values == null) return uri
        runBlocking(Dispatchers.IO) {
            val item =
                ClipboardItem(
                    text = values.getAsString("text").orEmpty(),
                    htmlText = values.getAsString("html_text"),
                    itemType = values.getAsInteger("item_type") ?: CLIP_ITEM_TYPE_TEXT,
                    entityType = values.getAsInteger("entity_type") ?: 0,
                    uri = values.getAsString("uri"),
                    sensitive = (values.getAsInteger("sensitive") ?: 0) != 0,
                    groupId = values.getAsInteger("group_id") ?: 0,
                    timestamp = values.getAsLong("timestamp") ?: System.currentTimeMillis(),
                )
            dao().insert(item)
        }
        return uri
    }

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<String>?,
    ): Int {
        val ctx = context ?: return 0
        ensureMatcher(clipboardAuthority(ctx))
        return runBlocking(Dispatchers.IO) {
            when (uriMatcher.match(uri)) {
                CLIP_DIR_CODE -> {
                    val items = dao().getAllSync()
                    items.forEach { dao().delete(it) }
                    items.size
                }

                CLIP_ITEM_CODE -> {
                    val id = ContentUris.parseId(uri).toInt()
                    val item = dao().getById(id) ?: return@runBlocking 0
                    dao().delete(item)
                    1
                }

                else -> {
                    0
                }
            }
        }
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?,
    ): Int {
        val ctx = context ?: return 0
        ensureMatcher(clipboardAuthority(ctx))
        if (values == null) return 0
        return runBlocking(Dispatchers.IO) {
            when (uriMatcher.match(uri)) {
                CLIP_ITEM_CODE -> {
                    val id = ContentUris.parseId(uri).toInt()
                    val existing = dao().getById(id) ?: return@runBlocking 0
                    dao().update(
                        existing.copy(
                            text = values.getAsString("text") ?: existing.text,
                            htmlText = values.getAsString("html_text") ?: existing.htmlText,
                            timestamp = values.getAsLong("timestamp") ?: existing.timestamp,
                        ),
                    )
                    1
                }

                CLIP_DIR_CODE -> {
                    0
                }

                else -> {
                    0
                }
            }
        }
    }
}
