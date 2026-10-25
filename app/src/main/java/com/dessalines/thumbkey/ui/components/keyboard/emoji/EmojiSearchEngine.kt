package com.dessalines.thumbkey.ui.components.keyboard.emoji

import java.util.Locale

object EmojiSearchEngine {
    @Volatile
    private var cache: Pair<String, List<EmojiEntry>>? = null

    fun normalize(query: String): String =
        query
            .lowercase(Locale.US)
            .trim()
            .replace("\\s+".toRegex(), " ")

    fun search(
        query: String,
        limit: Int = 200,
    ): List<EmojiEntry> {
        val normalized = normalize(query)
        if (normalized.isEmpty()) return emptyList()

        cache?.let { (cachedQuery, cachedResult) ->
            if (cachedQuery == normalized) return cachedResult.take(limit)
        }

        val tokens = normalized.split(" ")
        val scored =
            BUNDLED_EMOJI.mapNotNull { entry ->
                val score = matchScore(entry, tokens) ?: return@mapNotNull null
                entry to score
            }
        val result =
            scored
                .sortedWith(compareBy({ it.second }, { BUNDLED_EMOJI.indexOf(it.first) }))
                .map { it.first }

        cache = normalized to result
        return result.take(limit)
    }

    fun emojisByCategory(category: EmojiCategory): List<EmojiEntry> = BUNDLED_EMOJI.filter { it.category == category }

    fun clearCache() {
        cache = null
    }

    private fun matchScore(
        entry: EmojiEntry,
        tokens: List<String>,
    ): Int? {
        var total = 0
        for (token in tokens) {
            val tokenScore =
                when {
                    entry.emoji.contains(token) -> 0
                    entry.keywords.any { it == token } -> 1
                    entry.keywords.any { it.startsWith(token) } -> 2
                    entry.keywords.any { it.contains(token) } -> 3
                    else -> return null
                }
            total += tokenScore
        }
        return total
    }
}
