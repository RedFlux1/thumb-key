package com.dessalines.thumbkey.ui.components.keyboard.emoji

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EmojiPicker(
    onEmojiPicked: (String) -> Unit,
    recents: List<String>,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(EmojiCategory.SMILEYS) }

    val normalizedQuery = remember(query) { EmojiSearchEngine.normalize(query) }
    val results =
        remember(normalizedQuery) {
            if (normalizedQuery.isEmpty()) {
                emptyList()
            } else {
                EmojiSearchEngine.search(normalizedQuery)
            }
        }
    val showingSearch = normalizedQuery.isNotEmpty()

    val orderedCategories =
        remember(recents) {
            val base =
                listOf(
                    EmojiCategory.SMILEYS,
                    EmojiCategory.GESTURES,
                    EmojiCategory.HEARTS,
                    EmojiCategory.NATURE,
                    EmojiCategory.FOOD,
                    EmojiCategory.OBJECTS,
                    EmojiCategory.SYMBOLS,
                )
            if (recents.isEmpty()) base else listOf(EmojiCategory.RECENTS) + base
        }

    val gridItems: List<String> =
        when {
            showingSearch -> results.map { it.emoji }
            category == EmojiCategory.RECENTS -> recents
            else -> remember(category) { EmojiSearchEngine.emojisByCategory(category).map { it.emoji } }
        }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search emoji") },
            singleLine = true,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
        ) {
            if (showingSearch) {
                item(key = "search-tab") {
                    FilterChip(
                        selected = true,
                        onClick = {},
                        label = { Text("Results (${results.size})") },
                    )
                }
            } else {
                items(orderedCategories, key = { it.name }) { cat ->
                    FilterChip(
                        selected = cat == category,
                        onClick = { category = cat },
                        label = { Text(cat.label) },
                    )
                }
            }
        }
        if (gridItems.isEmpty()) {
            Text(
                text =
                    if (showingSearch) {
                        "No emojis found"
                    } else if (category == EmojiCategory.RECENTS) {
                        "No recent emojis yet"
                    } else {
                        "No emojis available"
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(8),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .weight(1f),
            ) {
                items(gridItems, key = { it }) { emoji ->
                    TextButton(onClick = { onEmojiPicked(emoji) }) {
                        Text(text = emoji, fontSize = 26.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}
