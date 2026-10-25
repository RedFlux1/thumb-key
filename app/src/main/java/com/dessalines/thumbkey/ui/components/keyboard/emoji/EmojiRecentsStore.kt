package com.dessalines.thumbkey.ui.components.keyboard.emoji

import android.content.Context

class EmojiRecentsStore(
    context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getRecents(): List<String> =
        prefs
            .getString(KEY_RECENTS, null)
            ?.split(DELIMITER)
            ?.filter { it.isNotEmpty() }
            ?.take(MAX_RECENTS)
            ?: emptyList()

    fun push(emoji: String) {
        if (emoji.isBlank()) return
        val updated =
            (listOf(emoji) + getRecents().filter { it != emoji })
                .take(MAX_RECENTS)
        prefs.edit().putString(KEY_RECENTS, updated.joinToString(DELIMITER)).apply()
    }

    companion object {
        private const val PREFS_NAME = "emoji_recents"
        private const val KEY_RECENTS = "recents"
        private const val DELIMITER = "\u001f"
        const val MAX_RECENTS = 40
    }
}
