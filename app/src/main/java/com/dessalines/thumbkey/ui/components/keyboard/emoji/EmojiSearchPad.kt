package com.dessalines.thumbkey.ui.components.keyboard.emoji

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.atan2

sealed interface PadSlot {
    data class Type(
        val text: String,
    ) : PadSlot

    data object Backspace : PadSlot

    data object CommitTop : PadSlot

    data object ExitEmoji : PadSlot

    data object Clear : PadSlot

    data object Empty : PadSlot
}

private data class PadKeyDef(
    val topLeft: PadSlot = PadSlot.Empty,
    val top: PadSlot = PadSlot.Empty,
    val topRight: PadSlot = PadSlot.Empty,
    val right: PadSlot = PadSlot.Empty,
    val bottomRight: PadSlot = PadSlot.Empty,
    val bottom: PadSlot = PadSlot.Empty,
    val bottomLeft: PadSlot = PadSlot.Empty,
    val left: PadSlot = PadSlot.Empty,
    val center: PadSlot = PadSlot.Empty,
)

private val PAD_KEYS =
    listOf(
        PadKeyDef(
            topLeft = PadSlot.Type("a"),
            top = PadSlot.Type("b"),
            topRight = PadSlot.Type("c"),
            right = PadSlot.Type("d"),
            bottomRight = PadSlot.Type("f"),
            bottom = PadSlot.Type("g"),
            bottomLeft = PadSlot.Type("h"),
            left = PadSlot.Type("i"),
            center = PadSlot.Type("e"),
        ),
        PadKeyDef(
            topLeft = PadSlot.Type("j"),
            top = PadSlot.Type("k"),
            topRight = PadSlot.Type("l"),
            right = PadSlot.Type("m"),
            bottomRight = PadSlot.Type("p"),
            bottom = PadSlot.Type("q"),
            bottomLeft = PadSlot.Type("r"),
            left = PadSlot.Type("s"),
            center = PadSlot.Type("o"),
        ),
        PadKeyDef(
            topLeft = PadSlot.Type("t"),
            top = PadSlot.Type("u"),
            topRight = PadSlot.Type("v"),
            right = PadSlot.Type("w"),
            bottomRight = PadSlot.Type("x"),
            bottom = PadSlot.Type("y"),
            bottomLeft = PadSlot.Type("z"),
            left = PadSlot.Type("n"),
            center = PadSlot.Type(" "),
        ),
        PadKeyDef(
            top = PadSlot.Backspace,
            right = PadSlot.Clear,
            bottom = PadSlot.CommitTop,
            left = PadSlot.ExitEmoji,
            center = PadSlot.Backspace,
        ),
    )

private fun PadSlot.label(): String =
    when (this) {
        is PadSlot.Type -> if (text == " ") "␣" else text
        PadSlot.Backspace -> "⌫"
        PadSlot.CommitTop -> "⏎"
        PadSlot.ExitEmoji -> "ABC"
        PadSlot.Clear -> "✕"
        PadSlot.Empty -> ""
    }

private fun PadKeyDef.slotForAngle(angleDeg: Double): PadSlot =
    when {
        angleDeg >= -22.5 && angleDeg < 22.5 -> right
        angleDeg >= 22.5 && angleDeg < 67.5 -> topRight
        angleDeg >= 67.5 && angleDeg < 112.5 -> top
        angleDeg >= 112.5 && angleDeg < 157.5 -> topLeft
        angleDeg >= 157.5 || angleDeg < -157.5 -> left
        angleDeg >= -157.5 && angleDeg < -112.5 -> bottomLeft
        angleDeg >= -112.5 && angleDeg < -67.5 -> bottom
        else -> bottomRight
    }

@Composable
fun EmojiSearchPad(
    query: String,
    onQueryChange: (String) -> Unit,
    onCommitTopResult: () -> Unit,
    onExitEmoji: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fire: (PadSlot) -> Unit = { slot ->
        when (slot) {
            is PadSlot.Type -> onQueryChange(query + slot.text)
            PadSlot.Backspace -> onQueryChange(query.dropLast(1))
            PadSlot.Clear -> onQueryChange("")
            PadSlot.CommitTop -> onCommitTopResult()
            PadSlot.ExitEmoji -> onExitEmoji()
            PadSlot.Empty -> Unit
        }
    }
    Column(modifier = modifier) {
        PAD_KEYS.forEach { key ->
            MiniPadKey(key = key, onFire = fire, modifier = Modifier.fillMaxWidth().weight(1f))
        }
    }
}

@Composable
private fun MiniPadKey(
    key: PadKeyDef,
    onFire: (PadSlot) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val thresholdPx =
        with(density) {
            12.dp.toPx()
        }
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .pointerInput(key) {
                    awaitEachGesture {
                        awaitFirstDown()
                        var total = Offset.Zero
                        var done = false
                        while (!done) {
                            val event = awaitPointerEvent()
                            for (change in event.changes) {
                                total += change.position - change.previousPosition
                            }
                            if (event.changes.all { !it.pressed }) done = true
                        }
                        if (total.getDistance() < thresholdPx) {
                            onFire(key.center)
                        } else {
                            val angle = atan2(-total.y, total.x) * 180.0 / Math.PI
                            onFire(key.slotForAngle(angle))
                        }
                    }
                },
    ) {
        MiniLabel(slot = key.topLeft, modifier = Modifier.align(Alignment.TopStart))
        MiniLabel(slot = key.top, modifier = Modifier.align(Alignment.TopCenter))
        MiniLabel(slot = key.topRight, modifier = Modifier.align(Alignment.TopEnd))
        MiniLabel(slot = key.left, modifier = Modifier.align(Alignment.CenterStart))
        MiniLabel(slot = key.right, modifier = Modifier.align(Alignment.CenterEnd))
        MiniLabel(slot = key.bottomLeft, modifier = Modifier.align(Alignment.BottomStart))
        MiniLabel(slot = key.bottom, modifier = Modifier.align(Alignment.BottomCenter))
        MiniLabel(slot = key.bottomRight, modifier = Modifier.align(Alignment.BottomEnd))
        val centerLabel = key.center.label()
        if (centerLabel.isNotEmpty()) {
            Text(
                text = centerLabel,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun MiniLabel(
    slot: PadSlot,
    modifier: Modifier = Modifier,
) {
    val label = slot.label()
    if (label.isNotEmpty() && slot != PadSlot.Empty) {
        Text(
            text = label,
            fontSize = 8.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = modifier,
        )
    }
}

@Composable
fun EmojiSearchPadSized(
    query: String,
    onQueryChange: (String) -> Unit,
    onCommitTopResult: () -> Unit,
    onExitEmoji: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EmojiSearchPad(
        query = query,
        onQueryChange = onQueryChange,
        onCommitTopResult = onCommitTopResult,
        onExitEmoji = onExitEmoji,
        modifier = modifier.width(56.dp).fillMaxHeight(),
    )
}
