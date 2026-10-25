package com.dessalines.thumbkey.ui.components.keyboard.emoji

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EmojiPicker(
    onEmojiPicked: (String) -> Unit,
    recents: List<String>,
    onExitEmoji: () -> Unit,
    vibrateOnTap: Boolean,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(EmojiCategory.SMILEYS) }
    val view = LocalView.current

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

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val padWidth = 56.dp
        val gridWidth = maxWidth - padWidth
        val columns = ((gridWidth / 48.dp).toInt()).coerceIn(3, 8)
        val emojiFontSize = ((gridWidth / columns).value * 0.52f).sp

        Row(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text("Search", fontSize = 13.sp) },
                    textStyle =
                        androidx.compose.ui.text
                            .TextStyle(fontSize = 14.sp),
                    singleLine = true,
                    trailingIcon =
                        if (query.isNotEmpty()) {
                            {
                                IconButton(onClick = {
                                    if (vibrateOnTap) {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    }
                                    query = ""
                                }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Clear search",
                                    )
                                }
                            }
                        } else {
                            null
                        },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp),
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
                        columns = GridCells.Fixed(columns),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .weight(1f),
                    ) {
                        items(gridItems, key = { it }) { emoji ->
                            TextButton(onClick = { onEmojiPicked(emoji) }) {
                                Text(text = emoji, fontSize = emojiFontSize, maxLines = 1)
                            }
                        }
                    }
                }
            }
            EmojiSearchPadSized(
                query = query,
                onQueryChange = { query = it },
                onCommitTopResult = {
                    results.firstOrNull()?.let { onEmojiPicked(it.emoji) }
                },
                onExitEmoji = onExitEmoji,
                modifier = Modifier.width(padWidth).fillMaxHeight(),
            )
        }
    }
}
