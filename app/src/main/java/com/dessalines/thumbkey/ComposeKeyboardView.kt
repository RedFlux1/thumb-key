package com.dessalines.thumbkey

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.dessalines.thumbkey.db.AppSettingsRepository
import com.dessalines.thumbkey.db.ClipboardRepository
import com.dessalines.thumbkey.db.DEFAULT_FLOATING_MODE_ENABLED
import com.dessalines.thumbkey.db.DEFAULT_PERSISTENT_FLOATING_ENABLED
import com.dessalines.thumbkey.ui.components.keyboard.KeyboardScreen
import com.dessalines.thumbkey.ui.theme.ThumbkeyTheme
import com.dessalines.thumbkey.utils.KeyboardMode
import com.dessalines.thumbkey.utils.KeyboardPosition
import com.dessalines.thumbkey.utils.keyboardLayoutsSetFromDbIndexString
import com.dessalines.thumbkey.utils.toBool
import com.dessalines.thumbkey.utils.toInt
import kotlinx.coroutines.launch

@SuppressLint("ViewConstructor")
class ComposeKeyboardView(
    context: Context,
    private val settingsRepo: AppSettingsRepository,
    private val clipboardRepo: ClipboardRepository,
    private val initialKeyboardMode: KeyboardMode? = null,
    private val forceDocked: Boolean = false,
) : AbstractComposeView(context) {
    @Composable
    override fun Content() {
        val settingsState = settingsRepo.appSettings.observeAsState()
        val settings by settingsState
        val ctx = context as IMEService

        val isFloating = !forceDocked && (settings?.floatingModeEnabled ?: DEFAULT_FLOATING_MODE_ENABLED).toBool()
        val isPersistent = isFloating && (settings?.persistentFloatingEnabled ?: DEFAULT_PERSISTENT_FLOATING_ENABLED).toBool()
        val isExpandInFloating = ctx.isExpandInFloatingEnabled()

        val keyboardContent = @Composable {
            KeyboardScreen(
                settings = settings,
                clipboardRepository = clipboardRepo,
                isFloating = isFloating,
                isExpandInFloating = isExpandInFloating,
                initialKeyboardMode = initialKeyboardMode,
                onSwitchLanguage = {
                    ctx.lifecycleScope.launch {
                        val state = settingsState.value
                        state?.let { s ->
                            val layouts =
                                keyboardLayoutsSetFromDbIndexString(s.keyboardLayouts).toList()
                            val currentLayout = s.keyboardLayout
                            val index = layouts.map { it.ordinal }.indexOf(currentLayout)
                            val nextIndex = (index + 1).mod(layouts.size)
                            val nextLayout = layouts.getOrNull(nextIndex)
                            nextLayout?.let { layout ->
                                val s2 = s.copy(keyboardLayout = layout.ordinal)
                                settingsRepo.update(s2)

                                ctx.currentKeyboardDefinition
                                    ?.settings
                                    ?.textProcessor
                                    ?.handleFinishInput(ctx)
                                ctx.currentKeyboardDefinition =
                                    layouts[nextIndex].keyboardDefinition
                                ctx.currentKeyboardDefinition
                                    ?.settings
                                    ?.textProcessor
                                    ?.updateCursorPosition(ctx)

                                if (s.showToastOnLayoutSwitch.toBool()) {
                                    val layoutName = layout.keyboardDefinition.title
                                    Toast
                                        .makeText(context, layoutName, Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }
                        }
                    }
                },
                onChangePosition = { f ->
                    ctx.lifecycleScope.launch {
                        val state = settingsState.value
                        state?.let { s ->
                            val nextPosition =
                                f(KeyboardPosition.entries[s.position]).ordinal
                            val s2 = s.copy(position = nextPosition)
                            settingsRepo.update(s2)
                        }
                    }
                },
                onToggleHideLetters = {
                    ctx.lifecycleScope.launch {
                        val state = settingsState.value
                        state?.let { s ->
                            val newHideLetters = (!s.hideLetters.toBool()).toInt()
                            val s2 = s.copy(hideLetters = newHideLetters)
                            settingsRepo.update(s2)
                        }
                    }
                },
                onGoToClipboardSettings = {
                    val intent =
                        Intent(context, MainActivity::class.java).apply {
                            putExtra("startRoute", "clipboardSettings")
                            addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK,
                            )
                        }
                    context.startActivity(intent)
                },
            )
        }

        ThumbkeyTheme(
            settings = settings,
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                if (isFloating) {
                    val widthFraction = ctx.maxWidthFraction() / 100f
                    val shortestSidePx =
                        minOf(
                            resources.displayMetrics.widthPixels,
                            resources.displayMetrics.heightPixels,
                        )
                    val maxWidthDp =
                        with(LocalDensity.current) { (shortestSidePx * widthFraction).toDp() }
                    FloatingKeyboardChrome(
                        ctx = ctx,
                        maxWidthDp = maxWidthDp,
                        isPersistent = isPersistent,
                        keyboardContent = keyboardContent,
                    )
                } else {
                    keyboardContent()
                }
            }
        }
    }
}

/**
 * Gboard-parity floating chrome.
 *
 * - Top drag handle moves the overlay (velocity-tracked, fling-down or
 *   drag-onto-dock dismisses, like `WidgetDragToMoveMotionEventHandler`).
 * - 8 resize handles scale the keyboard (corners both axes, edges one axis),
 *   range 0.5-2.0 like Gboard's `WidgetSoftKeyboardView.s` /
 *   `ScaledKeyboardViewInner`.
 * - Bottom dock target appears while dragging ("Drag here to exit floating
 *   mode", Gboard `floating_keyboard_dock_hint` parity).
 */
@Composable
private fun FloatingKeyboardChrome(
    ctx: IMEService,
    maxWidthDp: androidx.compose.ui.unit.Dp,
    isPersistent: Boolean,
    keyboardContent: @Composable () -> Unit,
) {
    val initialScale = ctx.floatingScale()
    var scaleX by remember { mutableStateOf(initialScale.first) }
    var scaleY by remember { mutableStateOf(initialScale.second) }
    var resizing by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var dockArmed by remember { mutableStateOf(false) }
    val dockArmPx =
        with(LocalDensity.current) { 96.dp.toPx() }

    fun persistScale() = ctx.saveFloatingScale(scaleX, scaleY)

    Box(
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier =
                Modifier
                    .widthIn(max = maxWidthDp)
                    .graphicsLayer(
                        scaleX = scaleX,
                        scaleY = scaleY,
                        alpha = if (resizing) 0.88f else 1f,
                    ),
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.background,
        ) {
            // SubcomposeLayout measures content without intrinsic queries,
            // which crash with AndroidView (EmojiPickerView) in overlays.
            SubcomposeLayout { constraints ->
                val measurables =
                    subcompose("floating_content") {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            // Drag handle: drag to reposition, fling-down or
                            // drag onto the dock bar to dismiss.
                            var totalDy by remember { mutableStateOf(0f) }
                            Box(
                                modifier =
                                    Modifier
                                        .width(48.dp)
                                        .height(24.dp)
                                        .pointerInput(Unit) {
                                            val velocityTracker = VelocityTracker()
                                            detectDragGestures(
                                                onDragStart = {
                                                    velocityTracker.resetTracking()
                                                    totalDy = 0f
                                                    dragging = true
                                                    dockArmed = false
                                                },
                                                onDragEnd = {
                                                    val velocity = velocityTracker.calculateVelocity()
                                                    dragging = false
                                                    if (velocity.y > FloatingKeyboardController.FLING_DISMISS_VELOCITY_Y ||
                                                        dockArmed
                                                    ) {
                                                        ctx.onFlingDismiss()
                                                    } else {
                                                        ctx.onDragEnd()
                                                    }
                                                    dockArmed = false
                                                },
                                                onDragCancel = {
                                                    dragging = false
                                                    dockArmed = false
                                                    ctx.onDragEnd()
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    velocityTracker.addPosition(
                                                        change.uptimeMillis,
                                                        change.position,
                                                    )
                                                    totalDy += dragAmount.y
                                                    dockArmed = totalDy > dockArmPx
                                                    ctx.onDrag(dragAmount.x, dragAmount.y)
                                                },
                                            )
                                        },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DragHandle,
                                    contentDescription = stringResource(R.string.floating_drag_handle),
                                    modifier = Modifier.height(12.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (isPersistent) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Outlined.PushPin,
                                        contentDescription = stringResource(R.string.persistent_floating_mode),
                                        modifier = Modifier.size(10.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            keyboardContent()
                            // Gboard dock-hint parity: drop target shown while dragging.
                            if (dragging) {
                                DockTarget(armed = dockArmed)
                            } else if (!remember { ctx.isDockHintShown() }) {
                                Text(
                                    text = stringResource(R.string.floating_dock_hint),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 4.dp),
                                )
                            }
                        }
                    }

                val placeables = measurables.map { it.measure(constraints) }
                val width = placeables.maxOf { it.width }
                val height = placeables.sumOf { it.height }

                layout(width, height) {
                    var y = 0
                    placeables.forEach { placeable ->
                        placeable.placeRelative(0, y)
                        y += placeable.height
                    }
                }
            }
        }
        // 8 Gboard-style resize handles around the keyboard bounds.
        val step = 700f
        ResizeHandle(
            description = stringResource(R.string.resize_top_left),
            modifier = Modifier.align(Alignment.TopStart).offset((-14).dp, (-14).dp),
            onResize = { dx, dy ->
                val d = ctx.floatingController.clampScale(scaleX + (dx + dy) / 2 / step)
                scaleX = d
                scaleY = ctx.floatingController.clampScale(scaleY + (dx + dy) / 2 / step)
            },
            onResizeEnd = {
                resizing = false
                persistScale()
            },
            onResizeStart = { resizing = true },
        )
        ResizeHandle(
            description = stringResource(R.string.resize_top_right),
            modifier = Modifier.align(Alignment.TopEnd).offset(14.dp, (-14).dp),
            onResize = { dx, dy ->
                val d = (-dx + dy) / 2 / step
                scaleX = ctx.floatingController.clampScale(scaleX + d)
                scaleY = ctx.floatingController.clampScale(scaleY + d)
            },
            onResizeEnd = {
                resizing = false
                persistScale()
            },
            onResizeStart = { resizing = true },
        )
        ResizeHandle(
            description = stringResource(R.string.resize_bottom_left),
            modifier = Modifier.align(Alignment.BottomStart).offset((-14).dp, 14.dp),
            onResize = { dx, dy ->
                val d = (dx - dy) / 2 / step
                scaleX = ctx.floatingController.clampScale(scaleX + d)
                scaleY = ctx.floatingController.clampScale(scaleY + d)
            },
            onResizeEnd = {
                resizing = false
                persistScale()
            },
            onResizeStart = { resizing = true },
        )
        ResizeHandle(
            description = stringResource(R.string.resize_bottom_right),
            modifier = Modifier.align(Alignment.BottomEnd).offset(14.dp, 14.dp),
            onResize = { dx, dy ->
                val d = (dx + dy) / 2 / step
                scaleX = ctx.floatingController.clampScale(scaleX + d)
                scaleY = ctx.floatingController.clampScale(scaleY + d)
            },
            onResizeEnd = {
                resizing = false
                persistScale()
            },
            onResizeStart = { resizing = true },
        )
        ResizeHandle(
            description = stringResource(R.string.resize_left),
            modifier = Modifier.align(Alignment.CenterStart).offset((-14).dp, 0.dp),
            onResize = { dx, _ ->
                scaleX = ctx.floatingController.clampScale(scaleX - dx / step)
            },
            onResizeEnd = {
                resizing = false
                persistScale()
            },
            onResizeStart = { resizing = true },
        )
        ResizeHandle(
            description = stringResource(R.string.resize_right),
            modifier = Modifier.align(Alignment.CenterEnd).offset(14.dp, 0.dp),
            onResize = { dx, _ ->
                scaleX = ctx.floatingController.clampScale(scaleX + dx / step)
            },
            onResizeEnd = {
                resizing = false
                persistScale()
            },
            onResizeStart = { resizing = true },
        )
        ResizeHandle(
            description = stringResource(R.string.resize_top),
            modifier = Modifier.align(Alignment.TopCenter).offset(0.dp, (-14).dp),
            onResize = { _, dy ->
                scaleY = ctx.floatingController.clampScale(scaleY - dy / step)
            },
            onResizeEnd = {
                resizing = false
                persistScale()
            },
            onResizeStart = { resizing = true },
        )
        ResizeHandle(
            description = stringResource(R.string.resize_bottom),
            modifier = Modifier.align(Alignment.BottomCenter).offset(0.dp, 14.dp),
            onResize = { _, dy ->
                scaleY = ctx.floatingController.clampScale(scaleY + dy / step)
            },
            onResizeEnd = {
                resizing = false
                persistScale()
            },
            onResizeStart = { resizing = true },
        )
    }
}

@Composable
private fun DockTarget(armed: Boolean) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .background(
                    color =
                        if (armed) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    shape = RoundedCornerShape(8.dp),
                ).padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.floating_dock_hint),
            style = MaterialTheme.typography.labelMedium,
            color =
                if (armed) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
    }
}

@Composable
private fun ResizeHandle(
    description: String,
    modifier: Modifier = Modifier,
    onResizeStart: () -> Unit = {},
    onResize: (dx: Float, dy: Float) -> Unit,
    onResizeEnd: () -> Unit = {},
) {
    Box(
        modifier =
            modifier
                .size(28.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { onResizeStart() },
                        onDragEnd = { onResizeEnd() },
                        onDragCancel = { onResizeEnd() },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onResize(dragAmount.x, dragAmount.y)
                        },
                    )
                },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(10.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = RoundedCornerShape(2.dp),
                    ),
        )
    }
}
