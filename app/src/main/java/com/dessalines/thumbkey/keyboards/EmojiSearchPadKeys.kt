package com.dessalines.thumbkey.keyboards

import com.dessalines.thumbkey.utils.ColorVariant.MUTED
import com.dessalines.thumbkey.utils.FontSizeVariant
import com.dessalines.thumbkey.utils.FontSizeVariant.LARGE
import com.dessalines.thumbkey.utils.KeyAction.EmojiSearchInput
import com.dessalines.thumbkey.utils.KeyC
import com.dessalines.thumbkey.utils.KeyDisplay
import com.dessalines.thumbkey.utils.KeyItemC

// Emoji search pad: the extra letter column from NUMERIC_KEYBOARD_NO_EXPANDED
// (NOThumbKeyDataDrivenExpanded numeric layer), with each slot typing into the
// emoji search field via KeyAction.EmojiSearchInput instead of committing text.
private fun emojiSearchKeyC(
    text: String,
    size: FontSizeVariant = FontSizeVariant.SMALL,
): KeyC =
    KeyC(
        action = EmojiSearchInput(text),
        display = KeyDisplay.TextDisplay(text),
        size = size,
        color = MUTED,
    )

val EMOJI_SEARCH_PAD_KEYS =
    listOf(
        KeyItemC(
            center = emojiSearchKeyC("a", LARGE),
            top = emojiSearchKeyC("b"),
            topRight = emojiSearchKeyC("c"),
            right = emojiSearchKeyC("d"),
            bottomRight = emojiSearchKeyC("e"),
            bottom = emojiSearchKeyC("f"),
            bottomLeft = emojiSearchKeyC("æ"),
            left = emojiSearchKeyC("w"),
        ),
        KeyItemC(
            center = emojiSearchKeyC("g", LARGE),
            top = emojiSearchKeyC("h"),
            topRight = emojiSearchKeyC("i"),
            right = emojiSearchKeyC("j"),
            bottomRight = emojiSearchKeyC("k"),
            bottom = emojiSearchKeyC("l"),
            bottomLeft = emojiSearchKeyC("ø"),
            left = emojiSearchKeyC("x"),
        ),
        KeyItemC(
            center = emojiSearchKeyC("m", LARGE),
            top = emojiSearchKeyC("n"),
            topRight = emojiSearchKeyC("o"),
            right = emojiSearchKeyC("p"),
            bottomRight = emojiSearchKeyC("q"),
            bottom = emojiSearchKeyC("r"),
            bottomLeft = emojiSearchKeyC("å"),
            left = emojiSearchKeyC("y"),
        ),
        KeyItemC(
            center = emojiSearchKeyC("s", LARGE),
            top = emojiSearchKeyC("t"),
            topRight = emojiSearchKeyC("u"),
            right = emojiSearchKeyC("v"),
            left = emojiSearchKeyC("z"),
        ),
    )
