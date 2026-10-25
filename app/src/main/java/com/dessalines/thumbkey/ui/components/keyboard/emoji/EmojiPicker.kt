package com.dessalines.thumbkey.ui.components.keyboard.emoji

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                ).clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            maxLines = 1,
            color =
                if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
    }
}

@Composable
fun EmojiPicker(
    onEmojiPicked: (String) -> Unit,
    recents: List<String>,
    query: String,
    onQueryChange: (String) -> Unit,
    searchPad: @Composable () -> Unit,
    vibrateOnTap: Boolean,
    modifier: Modifier = Modifier,
) {
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .height(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        BasicTextField(
                            value = query,
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 12.sp),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { innerTextField ->
                                if (query.isEmpty()) {
                                    Text(
                                        text = "Search",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    )
                                }
                                innerTextField()
                            },
                        )
                    }
                    if (query.isNotEmpty()) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier =
                                Modifier
                                    .size(22.dp)
                                    .clickable {
                                        if (vibrateOnTap) {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        }
                                        onQueryChange("")
                                    },
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                            .padding(horizontal = 6.dp),
                ) {
                    if (showingSearch) {
                        item(key = "search-tab") {
                            CategoryChip(label = "Results (${results.size})", selected = true, onClick = {})
                        }
                    } else {
                        items(orderedCategories, key = { it.name }) { cat ->
                            CategoryChip(
                                label = cat.label,
                                selected = cat == category,
                                onClick = { category = cat },
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
            Column(
                modifier =
                    Modifier
                        .width(padWidth)
                        .fillMaxHeight(),
            ) {
                searchPad()
            }
        }
    }
}
