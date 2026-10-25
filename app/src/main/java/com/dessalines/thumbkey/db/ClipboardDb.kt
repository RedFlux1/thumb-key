package com.dessalines.thumbkey.db

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Entity(
    tableName = "ClipboardItem",
    indices = [Index(value = ["text", "html_text", "uri"], unique = true)],
)
data class ClipboardItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "text")
    val text: String,
    @ColumnInfo(name = "html_text")
    val htmlText: String? = null,
    @ColumnInfo(name = "item_type", defaultValue = "0")
    val itemType: Int = CLIP_ITEM_TYPE_TEXT,
    @ColumnInfo(name = "entity_type", defaultValue = "0")
    val entityType: Int = 0,
    @ColumnInfo(name = "uri")
    val uri: String? = null,
    @ColumnInfo(name = "sensitive", defaultValue = "0")
    val sensitive: Boolean = false,
    @ColumnInfo(name = "group_id", defaultValue = "0")
    val groupId: Int = 0,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "is_pinned", defaultValue = "0")
    val isPinned: Boolean = false,
)

const val CLIP_ITEM_TYPE_TEXT = 0
const val CLIP_ITEM_TYPE_IMAGE = 1
const val CLIP_ITEM_TYPE_RICH = 2

@Dao
interface ClipboardItemDao {
    @Query("SELECT * FROM ClipboardItem ORDER BY is_pinned DESC, timestamp DESC")
    fun getAllClipboardItems(): LiveData<List<ClipboardItem>>

    @Query("SELECT * FROM ClipboardItem ORDER BY is_pinned DESC, timestamp DESC")
    suspend fun getAllSync(): List<ClipboardItem>

    @Query("SELECT * FROM ClipboardItem WHERE id = :id")
    suspend fun getById(id: Int): ClipboardItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ClipboardItem): Long

    @Update
    suspend fun update(item: ClipboardItem)

    @Delete
    suspend fun delete(item: ClipboardItem)

    @Query("DELETE FROM ClipboardItem WHERE is_pinned = 0")
    suspend fun clearUnpinnedItems()

    @Query("DELETE FROM ClipboardItem")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM ClipboardItem WHERE is_pinned = 0")
    suspend fun getUnpinnedCount(): Int

    @Query(
        "DELETE FROM ClipboardItem WHERE id IN " +
            "(SELECT id FROM ClipboardItem WHERE is_pinned = 0 ORDER BY timestamp ASC LIMIT :count)",
    )
    suspend fun deleteOldestUnpinned(count: Int)

    @Query("DELETE FROM ClipboardItem WHERE is_pinned = 0 AND timestamp < :cutoffTime")
    suspend fun deleteOlderThan(cutoffTime: Long)

    @Query("SELECT * FROM ClipboardItem WHERE text = :text LIMIT 1")
    suspend fun findByText(text: String): ClipboardItem?

    @Query(
        "SELECT * FROM ClipboardItem WHERE text = :text AND " +
            "IFNULL(html_text, '') = IFNULL(:htmlText, '') AND " +
            "IFNULL(uri, '') = IFNULL(:uri, '') LIMIT 1",
    )
    suspend fun findByUnique(
        text: String,
        htmlText: String?,
        uri: String?,
    ): ClipboardItem?

    @Query("SELECT * FROM ClipboardItem ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): ClipboardItem?
}

val CLIPBOARD_MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE ClipboardItem ADD COLUMN html_text TEXT")
            db.execSQL("ALTER TABLE ClipboardItem ADD COLUMN item_type INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE ClipboardItem ADD COLUMN entity_type INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE ClipboardItem ADD COLUMN uri TEXT")
            db.execSQL("ALTER TABLE ClipboardItem ADD COLUMN sensitive INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE ClipboardItem ADD COLUMN group_id INTEGER NOT NULL DEFAULT 0")
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_ClipboardItem_text_html_text_uri " +
                    "ON ClipboardItem(text, html_text, uri)",
            )
        }
    }

@Database(
    version = 2,
    entities = [ClipboardItem::class],
    exportSchema = true,
)
abstract class ClipboardDB : RoomDatabase() {
    abstract fun clipboardItemDao(): ClipboardItemDao

    companion object {
        @Volatile
        private var instance: ClipboardDB? = null

        fun getDatabase(context: Context): ClipboardDB =
            instance ?: synchronized(this) {
                val instance =
                    Room
                        .databaseBuilder(
                            context.applicationContext,
                            ClipboardDB::class.java,
                            "clipboard_db",
                        ).addMigrations(CLIPBOARD_MIGRATION_1_2)
                        .fallbackToDestructiveMigration(dropAllTables = true)
                        .build()
                Companion.instance = instance
                instance
            }
    }
}
