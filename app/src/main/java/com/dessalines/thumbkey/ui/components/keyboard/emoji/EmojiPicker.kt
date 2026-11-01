package com.dessalines.thumbkey.ui.components.keyboard.emoji

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
    modifier: Modifier = Modifier,
) {
    var category by remember { mutableStateOf(EmojiCategory.RECENTS) }

    val orderedCategories =
        remember {
            listOf(
                EmojiCategory.RECENTS,
                EmojiCategory.SMILEYS,
                EmojiCategory.PEOPLE,
                EmojiCategory.NATURE,
                EmojiCategory.FOOD,
                EmojiCategory.TRAVEL,
                EmojiCategory.ACTIVITY,
                EmojiCategory.OBJECTS,
                EmojiCategory.SYMBOLS,
                EmojiCategory.FLAGS,
            )
        }

    val gridItems: List<String> =
        if (category == EmojiCategory.RECENTS) {
            recents
        } else {
            remember(category) { EmojiSearchEngine.emojisByCategory(category).map { it.emoji } }
        }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = ((maxWidth / 48.dp).toInt()).coerceIn(3, 8)
        val emojiFontSize = ((maxWidth / columns).value * 0.52f).sp

        Column(modifier = Modifier.fillMaxSize()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .padding(start = 6.dp, end = 6.dp, top = 6.dp),
            ) {
                items(orderedCategories, key = { it.name }) { cat ->
                    CategoryChip(
                        label = cat.label,
                        selected = cat == category,
                        onClick = { category = cat },
                    )
                }
            }
            if (gridItems.isEmpty()) {
                Text(
                    text =
                        if (category == EmojiCategory.RECENTS) {
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
    }
}
