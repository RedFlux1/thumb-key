package com.dessalines.thumbkey.db

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import com.dessalines.thumbkey.utils.toBool

class ClipboardRepository(
    private val clipboardItemDao: ClipboardItemDao,
    private val appSettingsDao: AppSettingsDao,
) {
    val allClipboardItems: LiveData<List<ClipboardItem>> = clipboardItemDao.getAllClipboardItems()

    @WorkerThread
    suspend fun addItem(text: String) {
        addItem(text = text)
    }

    @WorkerThread
    suspend fun addItem(
        text: String,
        htmlText: String? = null,
        itemType: Int = CLIP_ITEM_TYPE_TEXT,
        entityType: Int = 0,
        uri: String? = null,
        sensitive: Boolean = false,
        groupId: Int = 0,
        timestamp: Long = System.currentTimeMillis(),
    ) {
        val settings = appSettingsDao.getSettingsSync()
        if (settings?.clipboardHistoryEnabled?.toBool() != true) return

        clearExpired()

        val existingItem = clipboardItemDao.findByUnique(text, htmlText, uri)
        if (existingItem != null) {
            clipboardItemDao.update(
                existingItem.copy(
                    timestamp = timestamp,
                    itemType = itemType,
                    entityType = entityType,
                    sensitive = sensitive,
                    groupId = groupId,
                ),
            )
            return
        }

        val item =
            ClipboardItem(
                text = text,
                htmlText = htmlText,
                itemType = itemType,
                entityType = entityType,
                uri = uri,
                sensitive = sensitive,
                groupId = groupId,
                timestamp = timestamp,
            )
        clipboardItemDao.insert(item)

        if (settings.clipboardSizeLimitEnabled.toBool()) {
            enforceSizeLimit(settings.clipboardMaxSize)
        }
    }

    @WorkerThread
    suspend fun getLatest(): ClipboardItem? = clipboardItemDao.getLatest()

    @WorkerThread
    suspend fun getAllUris(): List<String> = clipboardItemDao.getAllUris()

    @WorkerThread
    suspend fun togglePin(item: ClipboardItem) {
        clipboardItemDao.update(item.copy(isPinned = !item.isPinned))
    }

    @WorkerThread
    suspend fun deleteItem(item: ClipboardItem) {
        clipboardItemDao.delete(item)
    }

    @WorkerThread
    suspend fun clearAll() {
        clipboardItemDao.clearAll()
    }

    @WorkerThread
    suspend fun clearUnpinned() {
        clipboardItemDao.clearUnpinnedItems()
    }

    @WorkerThread
    suspend fun clearExpired() {
        val settings = appSettingsDao.getSettingsSync() ?: return
        if (!settings.clipboardAutoCleanupEnabled.toBool()) return

        val cutoffTime = System.currentTimeMillis() - (settings.clipboardCleanupAfterMinutes * 60 * 1000L)
        clipboardItemDao.deleteOlderThan(cutoffTime)
    }

    private suspend fun enforceSizeLimit(maxSize: Int) {
        val unpinnedCount = clipboardItemDao.getUnpinnedCount()
        if (unpinnedCount > maxSize) {
            clipboardItemDao.deleteOldestUnpinned(unpinnedCount - maxSize)
        }
    }

    @WorkerThread
    suspend fun enforceSizeLimit() {
        val settings = appSettingsDao.getSettingsSync() ?: return
        if (!settings.clipboardSizeLimitEnabled.toBool()) return
        enforceSizeLimit(settings.clipboardMaxSize)
    }
}
