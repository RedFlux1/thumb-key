@file:Suppress("ktlint:standard:no-wildcard-imports")

package com.dessalines.thumbkey.keyboards

import android.view.KeyEvent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import com.dessalines.thumbkey.utils.*
import com.dessalines.thumbkey.utils.ColorVariant.*
import com.dessalines.thumbkey.utils.FontSizeVariant.*
import com.dessalines.thumbkey.utils.KeyAction.*
import com.dessalines.thumbkey.utils.SwipeNWay.*

// Norwegian data-driven expanded layout (5×4 grid with number column).
// Numbers on the left, arrow-key spacebar with Ctrl/Alt, Escape/Tab.
// Programming symbols live on the numeric layer.

// ═══════════════════════════════════════════════════
//  Shared key items (reused from NOThumbKeyDataDriven)
// ═══════════════════════════════════════════════════

private val SPACEBAR_DATADRIVEN_TOP_KEYC =
    KeyC(
        action =
            SendEvent(
                KeyEvent(
                    KeyEvent.ACTION_DOWN,
                    KeyEvent.KEYCODE_DPAD_UP,
                ),
            ),
        display = null,
    )

private val SPACEBAR_DATADRIVEN_BOTTOM_KEYC =
    KeyC(
        action =
            SendEvent(
                KeyEvent(
                    KeyEvent.ACTION_DOWN,
                    KeyEvent.KEYCODE_DPAD_DOWN,
                ),
            ),
        display = null,
    )

val SPACEBAR_DATADRIVEN_EXPANDED_KEY_ITEM =
    KeyItemC(
        center = SPACEBAR_CENTER_KEYC,
        swipeType = EIGHT_WAY,
        slideType = SlideType.MOVE_CURSOR,
        top = SPACEBAR_DATADRIVEN_TOP_KEYC,
        bottom = SPACEBAR_DATADRIVEN_BOTTOM_KEYC,
        left = SPACEBAR_LEFT_KEYC,
        right = SPACEBAR_RIGHT_KEYC,
        topLeft = TOGGLE_CTRL_TRUE_KEYC,
        topRight = TOGGLE_ALT_TRUE_KEYC,
        bottomLeft = PREVIOUS_WORD_BEFORE_CURSOR_KEYC,
        bottomRight = NEXT_WORD_AFTER_CURSOR_KEYC,
        backgroundColor = SURFACE_VARIANT,
        widthMultiplier = 3,
    )

val RETURN_DATADRIVEN_EXPANDED_KEY_ITEM =
    KeyItemC(
        center = RETURN_KEYC,
        swipeType = FOUR_WAY_CROSS,
        top =
            KeyC(
                action =
                    SendEvent(
                        KeyEvent(
                            KeyEvent.ACTION_DOWN,
                            KeyEvent.KEYCODE_ESCAPE,
                        ),
                    ),
                display = null,
            ),
        left =
            KeyC(
                action =
                    SendEvent(
                        KeyEvent(
                            KeyEvent.ACTION_DOWN,
                            KeyEvent.KEYCODE_MOVE_HOME,
                        ),
                    ),
                display = null,
            ),
        right =
            KeyC(
                action =
                    SendEvent(
                        KeyEvent(
                            KeyEvent.ACTION_DOWN,
                            KeyEvent.KEYCODE_MOVE_END,
                        ),
                    ),
                display = null,
            ),
        bottom = TOGGLE_FN_TRUE_KEYC,
        backgroundColor = SURFACE_VARIANT,
        longPress = CommitText("\n"),
    )

// ══════════════════════════
//  MAIN layer (5 columns)
// ══════════════════════════

val KB_NO_THUMBKEY_DATADRIVEN_EXPANDED_MAIN =
    KeyboardC(
        listOf(
            listOf(
                KeyItemC(
                    center = KeyC("3", size = LARGE, color = MUTED),
                    swipeType = FOUR_WAY_CROSS,
                    right = KeyC("9", color = MUTED),
                    bottom = KeyC("6", color = MUTED),
                ),
                KeyItemC(
                    center = KeyC("n", size = LARGE),
                    swipeType = FOUR_WAY_DIAGONAL,
                    bottomRight = KeyC("v"),
                ),
                KeyItemC(
                    center = KeyC("l", size = LARGE),
                    bottom = KeyC("m"),
                ),
                KeyItemC(
                    center = KeyC("a", size = LARGE),
                    swipeType = FOUR_WAY_DIAGONAL,
                    bottomLeft = KeyC("u"),
                ),
                EMOJI_KEY_ITEM,
            ),
            listOf(
                KeyItemC(
                    center = KeyC("2", size = LARGE, color = MUTED),
                    swipeType = FOUR_WAY_CROSS,
                    right = KeyC("8", color = MUTED),
                    bottom = KeyC("5", color = MUTED),
                ),
                KeyItemC(
                    center = KeyC("t", size = LARGE),
                    swipeType = TWO_WAY_HORIZONTAL,
                    right = KeyC("d"),
                ),
                KeyItemC(
                    center = KeyC("s", size = LARGE),
                    topLeft = KeyC("h"),
                    top = KeyC("c"),
                    topRight = KeyC("y"),
                    right = KeyC("å"),
                    bottomRight = KeyC("j"),
                    bottom = KeyC("b"),
                    bottomLeft = KeyC("ø"),
                    left = KeyC("æ"),
                ),
                KeyItemC(
                    center = KeyC("i", size = LARGE),
                    swipeType = FOUR_WAY_CROSS,
                    left = KeyC("p"),
                    top =
                        KeyC(
                            display = KeyDisplay.IconDisplay(Icons.Outlined.ArrowDropUp),
                            action = ToggleShiftMode(true),
                            swipeReturnAction = ToggleCurrentWordCapitalization(true),
                            color = MUTED,
                        ),
                    bottom =
                        KeyC(
                            ToggleShiftMode(false),
                            swipeReturnAction = ToggleCurrentWordCapitalization(false),
                        ),
                ),
                NUMERIC_KEY_ITEM,
            ),
            listOf(
                KeyItemC(
                    center = KeyC("1", size = LARGE, color = MUTED),
                    swipeType = FOUR_WAY_CROSS,
                    right = KeyC("7", color = MUTED),
                    bottom = KeyC("4", color = MUTED),
                ),
                KeyItemC(
                    center = KeyC("r", size = LARGE),
                    topRight = KeyC("g"),
                    right = KeyC("z"),
                ),
                KeyItemC(
                    center = KeyC("o", size = LARGE),
                    top = KeyC("k"),
                    topRight = KeyC("'", color = MUTED),
                    right = KeyC("w"),
                    bottomRight = KeyC("-", color = MUTED),
                    bottom = KeyC(".", color = MUTED),
                    bottomLeft = KeyC("*", color = MUTED),
                ),
                KeyItemC(
                    center = KeyC("e", size = LARGE),
                    topLeft = KeyC("f"),
                    top = KeyC("q"),
                    left = KeyC("x"),
                ),
                BACKSPACE_KEY_ITEM,
            ),
            listOf(
                KeyItemC(
                    center = KeyC("0", size = LARGE, color = MUTED),
                ),
                SPACEBAR_DATADRIVEN_EXPANDED_KEY_ITEM,
                RETURN_DATADRIVEN_EXPANDED_KEY_ITEM,
            ),
        ),
    )

// ════════════════════════════
//  SHIFTED layer (5 columns)
// ════════════════════════════

val KB_NO_THUMBKEY_DATADRIVEN_EXPANDED_SHIFTED =
    KeyboardC(
        listOf(
            listOf(
                KeyItemC(
                    center = KeyC("3", size = LARGE, color = MUTED),
                    swipeType = FOUR_WAY_CROSS,
                    right = KeyC("9", color = MUTED),
                    bottom = KeyC("6", color = MUTED),
                ),
                KeyItemC(
                    center = KeyC("N", size = LARGE),
                    swipeType = FOUR_WAY_DIAGONAL,
                    bottomRight = KeyC("V"),
                ),
                KeyItemC(
                    center = KeyC("L", size = LARGE),
                    bottom = KeyC("M"),
                ),
                KeyItemC(
                    center = KeyC("A", size = LARGE),
                    swipeType = FOUR_WAY_DIAGONAL,
                    bottomLeft = KeyC("U"),
                ),
                EMOJI_KEY_ITEM,
            ),
            listOf(
                KeyItemC(
                    center = KeyC("2", size = LARGE, color = MUTED),
                    swipeType = FOUR_WAY_CROSS,
                    right = KeyC("8", color = MUTED),
                    bottom = KeyC("5", color = MUTED),
                ),
                KeyItemC(
                    center = KeyC("T", size = LARGE),
                    swipeType = TWO_WAY_HORIZONTAL,
                    right = KeyC("D"),
                ),
                KeyItemC(
                    center = KeyC("S", size = LARGE),
                    topLeft = KeyC("H"),
                    top = KeyC("C"),
                    topRight = KeyC("Y"),
                    right = KeyC("Å"),
                    bottomRight = KeyC("J"),
                    bottom = KeyC("B"),
                    bottomLeft = KeyC("Ø"),
                    left = KeyC("Æ"),
                ),
                KeyItemC(
                    center = KeyC("I", size = LARGE),
                    swipeType = FOUR_WAY_CROSS,
                    left = KeyC("P"),
                    bottom =
                        KeyC(
                            display = KeyDisplay.IconDisplay(Icons.Outlined.ArrowDropDown),
                            action = ToggleShiftMode(false),
                            swipeReturnAction = ToggleCurrentWordCapitalization(false),
                            color = MUTED,
                        ),
                    top =
                        KeyC(
                            display = KeyDisplay.IconDisplay(Icons.Outlined.KeyboardCapslock),
                            capsModeDisplay = KeyDisplay.IconDisplay(Icons.Outlined.Copyright),
                            action = ToggleCapsLock,
                            swipeReturnAction = ToggleCurrentWordCapitalization(true),
                            color = MUTED,
                        ),
                ),
                NUMERIC_KEY_ITEM,
            ),
            listOf(
                KeyItemC(
                    center = KeyC("1", size = LARGE, color = MUTED),
                    swipeType = FOUR_WAY_CROSS,
                    right = KeyC("7", color = MUTED),
                    bottom = KeyC("4", color = MUTED),
                ),
                KeyItemC(
                    center = KeyC("R", size = LARGE),
                    topRight = KeyC("G"),
                    right = KeyC("Z"),
                ),
                KeyItemC(
                    center = KeyC("O", size = LARGE),
                    top = KeyC("K"),
                    topRight = KeyC("'", color = MUTED),
                    right = KeyC("W"),
                    bottomRight = KeyC("-", color = MUTED),
                    bottom = KeyC(".", color = MUTED),
                    bottomLeft = KeyC("*", color = MUTED),
                ),
                KeyItemC(
                    center = KeyC("E", size = LARGE),
                    topLeft = KeyC("F"),
                    top = KeyC("Q"),
                    left = KeyC("X"),
                ),
                BACKSPACE_KEY_ITEM,
            ),
            listOf(
                KeyItemC(
                    center = KeyC("0", size = LARGE, color = MUTED),
                ),
                SPACEBAR_DATADRIVEN_EXPANDED_KEY_ITEM,
                RETURN_DATADRIVEN_EXPANDED_KEY_ITEM,
            ),
        ),
    )

// ════════════════════════════════════════════════════
//  NUMERIC layer — numbers + all Norwegian letters
// ════════════════════════════════════════════════════

val NUMERIC_KEYBOARD_NO_EXPANDED =
    KeyboardC(
        listOf(
            listOf(
                KeyItemC(
                    center = KeyC("a", size = LARGE, color = MUTED),
                    top = KeyC("b", color = MUTED),
                    topRight = KeyC("c", color = MUTED),
                    right = KeyC("d", color = MUTED),
                    bottomRight = KeyC("e", color = MUTED),
                    bottom = KeyC("f", color = MUTED),
                    bottomLeft = KeyC("æ", color = MUTED),
                    left = KeyC("w", color = MUTED),
                ),
                KeyItemC(
                    center = KeyC("1", size = LARGE),
                    left =
                        KeyC(
                            display = KeyDisplay.IconDisplay(Icons.AutoMirrored.Outlined.KeyboardTab),
                            action =
                                SendEvent(
                                    KeyEvent(
                                        0L,
                                        0L,
                                        KeyEvent.ACTION_DOWN,
                                        KeyEvent.KEYCODE_TAB,
                                        0,
                                        KeyEvent.META_SHIFT_ON,
                                    ),
                                ),
                            color = MUTED,
                        ),
                    bottomLeft = KeyC("$"),
                ),
                KeyItemC(
                    center = KeyC("2", size = LARGE),
                    topLeft = KeyC("`"),
                    top = KeyC("^"),
                    topRight = KeyC("´"),
                    right = KeyC("!"),
                    bottomRight = KeyC("\\"),
                    bottomLeft = KeyC("/"),
                    left = KeyC("+"),
                ),
                KeyItemC(
                    center = KeyC("3", size = LARGE),
                    left = KeyC("?"),
                    right =
                        KeyC(
                            display = KeyDisplay.IconDisplay(Icons.AutoMirrored.Outlined.KeyboardTab),
                            action = CommitText("\t"),
                            color = MUTED,
                        ),
                    bottomRight = KeyC("€"),
                    bottomLeft = KeyC("£"),
                    bottom = KeyC("="),
                ),
                EMOJI_KEY_ITEM,
            ),
            listOf(
                KeyItemC(
                    center = KeyC("g", size = LARGE, color = MUTED),
                    top = KeyC("h", color = MUTED),
                    topRight = KeyC("i", color = MUTED),
                    right = KeyC("j", color = MUTED),
                    bottomRight = KeyC("k", color = MUTED),
                    bottom = KeyC("l", color = MUTED),
                    bottomLeft = KeyC("ø", color = MUTED),
                    left = KeyC("x", color = MUTED),
                ),
                KeyItemC(
                    center = KeyC("4", size = LARGE),
                    topLeft = KeyC("{"),
                    topRight = KeyC("%"),
                    bottomRight = KeyC("_"),
                    bottomLeft = KeyC("["),
                    left = KeyC("("),
                ),
                KeyItemC(
                    center = KeyC("5", size = LARGE),
                ),
                KeyItemC(
                    center = KeyC("6", size = LARGE),
                    topLeft = KeyC("|"),
                    topRight = KeyC("}"),
                    right = KeyC(")"),
                    bottomRight = KeyC("]"),
                    bottomLeft = KeyC("@"),
                    top =
                        KeyC(
                            display = KeyDisplay.IconDisplay(Icons.Outlined.ArrowDropUp),
                            action = ToggleAltMode(true),
                            color = MUTED,
                        ),
                    bottom =
                        KeyC(
                            display = KeyDisplay.IconDisplay(Icons.Outlined.ArrowDropDown),
                            action = ToggleNumericMode(true),
                            color = MUTED,
                        ),
                ),
                ABC_KEY_ITEM,
            ),
            listOf(
                KeyItemC(
                    center = KeyC("m", size = LARGE, color = MUTED),
                    top = KeyC("n", color = MUTED),
                    topRight = KeyC("o", color = MUTED),
                    right = KeyC("p", color = MUTED),
                    bottomRight = KeyC("q", color = MUTED),
                    bottom = KeyC("r", color = MUTED),
                    bottomLeft = KeyC("å", color = MUTED),
                    left = KeyC("y", color = MUTED),
                ),
                KeyItemC(
                    center = KeyC("7", size = LARGE),
                    topLeft = KeyC("~"),
                    bottomRight = KeyC(":"),
                    bottomLeft = KeyC("<"),
                ),
                KeyItemC(
                    center = KeyC("8", size = LARGE),
                    topLeft = KeyC("\""),
                    topRight = KeyC("'"),
                    bottomRight = KeyC("-"),
                    bottom = KeyC("."),
                    bottomLeft = KeyC("*"),
                    left = KeyC(","),
                ),
                KeyItemC(
                    center = KeyC("9", size = LARGE),
                    top = KeyC("&"),
                    topRight = KeyC("°"),
                    bottomRight = KeyC(">"),
                    bottomLeft = KeyC(";"),
                    left = KeyC("#"),
                ),
                BACKSPACE_KEY_ITEM,
            ),
            listOf(
                KeyItemC(
                    center = KeyC("s", size = LARGE, color = MUTED),
                    top = KeyC("t", color = MUTED),
                    topRight = KeyC("u", color = MUTED),
                    right = KeyC("v", color = MUTED),
                    left = KeyC("z", color = MUTED),
                ),
                KeyItemC(
                    center = KeyC("0", size = LARGE),
                    widthMultiplier = 2,
                ),
                SPACEBAR_SKINNY_KEY_ITEM,
                RETURN_KEY_ITEM,
            ),
        ),
    )

// ══════════════════════════
//  KeyboardDefinition
// ══════════════════════════

val KB_NO_THUMBKEY_DATADRIVEN_EXPANDED: KeyboardDefinition =
    KeyboardDefinition(
        title = "norsk thumb-key datadrevet utvidet",
        modes =
            KeyboardDefinitionModes(
                main = KB_NO_THUMBKEY_DATADRIVEN_EXPANDED_MAIN,
                shifted = KB_NO_THUMBKEY_DATADRIVEN_EXPANDED_SHIFTED,
                numeric = NUMERIC_KEYBOARD_NO_EXPANDED,
            ),
    )
