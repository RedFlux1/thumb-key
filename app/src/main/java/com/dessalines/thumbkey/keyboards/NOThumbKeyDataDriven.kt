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

// Norwegian data-driven programming layout —
// programming symbols on swipes, arrow-key spacebar, Ctrl/Alt modifiers, Escape, Tab.

// ── Custom spacebar: arrow keys on cardinals, Ctrl/Alt on diagonals ──

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

val SPACEBAR_DATADRIVEN_KEY_ITEM =
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

// ── Custom return: Escape on top ──

val RETURN_DATADRIVEN_KEY_ITEM =
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

val KB_NO_THUMBKEY_DATADRIVEN_MAIN =
    KeyboardC(
        listOf(
            listOf(
                KeyItemC(
                    center = KeyC("n", size = LARGE),
                    swipeType = FOUR_WAY_DIAGONAL,
                    bottomRight = KeyC("v"),
                    topLeft = KeyC("/", color = MUTED),
                    topRight = KeyC("\\", color = MUTED),
                    bottomLeft = KeyC("(", color = MUTED),
                ),
                KeyItemC(
                    center = KeyC("l", size = LARGE),
                    bottom = KeyC("m"),
                    top = KeyC("^", color = MUTED),
                    left = KeyC("<", color = MUTED),
                    right = KeyC(">", color = MUTED),
                    topLeft = KeyC("\"", color = MUTED),
                ),
                KeyItemC(
                    center = KeyC("a", size = LARGE),
                    swipeType = FOUR_WAY_DIAGONAL,
                    bottomLeft = KeyC("u"),
                    topLeft = KeyC("=", color = MUTED),
                    topRight = KeyC("+", color = MUTED),
                    bottomRight = KeyC(")", color = MUTED),
                ),
                EMOJI_KEY_ITEM,
            ),
            listOf(
                KeyItemC(
                    center = KeyC("t", size = LARGE),
                    right = KeyC("d"),
                    top = KeyC("@", color = MUTED),
                    bottom = KeyC("$", color = MUTED),
                    topLeft = KeyC("#", color = MUTED),
                    topRight = KeyC("!", color = MUTED),
                    bottomLeft = KeyC("|", color = MUTED),
                    bottomRight = KeyC("%", color = MUTED),
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
                    right =
                        KeyC(
                            display = KeyDisplay.IconDisplay(Icons.AutoMirrored.Outlined.KeyboardTab),
                            action = CommitText("\t"),
                            color = MUTED,
                        ),
                ),
                NUMERIC_KEY_ITEM,
            ),
            listOf(
                KeyItemC(
                    center = KeyC("r", size = LARGE),
                    topRight = KeyC("g"),
                    right = KeyC("z"),
                    topLeft = KeyC("{", color = MUTED),
                    bottomLeft = KeyC("[", color = MUTED),
                    bottom = KeyC(",", color = MUTED),
                    bottomRight = KeyC(";", color = MUTED),
                ),
                KeyItemC(
                    center = KeyC("o", size = LARGE),
                    top = KeyC("k"),
                    topRight = KeyC("'", color = MUTED),
                    right = KeyC("w"),
                    bottomRight = KeyC("-", color = MUTED),
                    bottom = KeyC(".", color = MUTED),
                    bottomLeft = KeyC("*", color = MUTED),
                    left = KeyC("&", color = MUTED),
                ),
                KeyItemC(
                    center = KeyC("e", size = LARGE),
                    topLeft = KeyC("f"),
                    top = KeyC("q"),
                    left = KeyC("x"),
                    bottomLeft = KeyC(":", color = MUTED),
                    topRight = KeyC("}", color = MUTED),
                    bottomRight = KeyC("]", color = MUTED),
                ),
                BACKSPACE_KEY_ITEM,
            ),
            listOf(
                SPACEBAR_DATADRIVEN_KEY_ITEM,
                RETURN_DATADRIVEN_KEY_ITEM,
            ),
        ),
    )

val KB_NO_THUMBKEY_DATADRIVEN_SHIFTED =
    KeyboardC(
        listOf(
            listOf(
                KeyItemC(
                    center = KeyC("N", size = LARGE),
                    swipeType = FOUR_WAY_DIAGONAL,
                    bottomRight = KeyC("V"),
                    topLeft = KeyC("~", color = MUTED),
                    topRight = KeyC("\\", color = MUTED),
                    bottomLeft = KeyC("(", color = MUTED),
                ),
                KeyItemC(
                    center = KeyC("L", size = LARGE),
                    bottom = KeyC("M"),
                    top = KeyC("^", color = MUTED),
                    left = KeyC("<", color = MUTED),
                    right = KeyC(">", color = MUTED),
                    topLeft = KeyC("\"", color = MUTED),
                ),
                KeyItemC(
                    center = KeyC("A", size = LARGE),
                    swipeType = FOUR_WAY_DIAGONAL,
                    bottomLeft = KeyC("U"),
                    topLeft = KeyC("=", color = MUTED),
                    topRight = KeyC("+", color = MUTED),
                    bottomRight = KeyC(")", color = MUTED),
                ),
                EMOJI_KEY_ITEM,
            ),
            listOf(
                KeyItemC(
                    center = KeyC("T", size = LARGE),
                    right = KeyC("D"),
                    top = KeyC("@", color = MUTED),
                    bottom = KeyC("€", color = MUTED),
                    topLeft = KeyC("#", color = MUTED),
                    topRight = KeyC("!", color = MUTED),
                    bottomLeft = KeyC("|", color = MUTED),
                    bottomRight = KeyC("%", color = MUTED),
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
                    right =
                        KeyC(
                            display = KeyDisplay.IconDisplay(Icons.AutoMirrored.Outlined.KeyboardTab),
                            action = CommitText("\t"),
                            color = MUTED,
                        ),
                ),
                NUMERIC_KEY_ITEM,
            ),
            listOf(
                KeyItemC(
                    center = KeyC("R", size = LARGE),
                    topRight = KeyC("G"),
                    right = KeyC("Z"),
                    topLeft = KeyC("{", color = MUTED),
                    bottomLeft = KeyC("[", color = MUTED),
                    bottom = KeyC(",", color = MUTED),
                    bottomRight = KeyC(";", color = MUTED),
                ),
                KeyItemC(
                    center = KeyC("O", size = LARGE),
                    top = KeyC("K"),
                    topRight = KeyC("'", color = MUTED),
                    right = KeyC("W"),
                    bottomRight = KeyC("_", color = MUTED),
                    bottom = KeyC(".", color = MUTED),
                    bottomLeft = KeyC("*", color = MUTED),
                    left = KeyC("&", color = MUTED),
                ),
                KeyItemC(
                    center = KeyC("E", size = LARGE),
                    topLeft = KeyC("F"),
                    top = KeyC("Q"),
                    left = KeyC("X"),
                    bottomLeft = KeyC(":", color = MUTED),
                    topRight = KeyC("}", color = MUTED),
                    bottomRight = KeyC("]", color = MUTED),
                ),
                BACKSPACE_KEY_ITEM,
            ),
            listOf(
                SPACEBAR_DATADRIVEN_KEY_ITEM,
                RETURN_DATADRIVEN_KEY_ITEM,
            ),
        ),
    )

val KB_NO_THUMBKEY_DATADRIVEN: KeyboardDefinition =
    KeyboardDefinition(
        title = "norsk thumb-key datadrevet programmering",
        modes =
            KeyboardDefinitionModes(
                main = KB_NO_THUMBKEY_DATADRIVEN_MAIN,
                shifted = KB_NO_THUMBKEY_DATADRIVEN_SHIFTED,
                numeric = NUMERIC_KEYBOARD,
            ),
    )
