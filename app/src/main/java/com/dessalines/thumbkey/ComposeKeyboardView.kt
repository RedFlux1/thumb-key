package com.dessalines.thumbkey

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
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
                    Surface(
                        modifier = Modifier.widthIn(max = maxWidthDp),
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
                                        // Drag handle: drag to reposition, fling-down to dismiss
                                        Box(
                                            modifier =
                                                Modifier
                                                    .width(48.dp)
                                                    .height(24.dp)
                                                    .pointerInput(Unit) {
                                                        val velocityTracker = VelocityTracker()
                                                        detectDragGestures(
                                                            onDragStart = { velocityTracker.resetTracking() },
                                                            onDragEnd = {
                                                                val velocity = velocityTracker.calculateVelocity()
                                                                if (velocity.y > 2500f) {
                                                                    ctx.onFlingDismiss()
                                                                } else {
                                                                    ctx.onDragEnd()
                                                                }
                                                            },
                                                            onDragCancel = { ctx.onDragEnd() },
                                                            onDrag = { change, dragAmount ->
                                                                change.consume()
                                                                velocityTracker.addPosition(
                                                                    change.uptimeMillis,
                                                                    change.position,
                                                                )
                                                                ctx.onDrag(dragAmount.x, dragAmount.y)
                                                            },
                                                        )
                                                    },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.DragHandle,
                                                contentDescription = null,
                                                modifier = Modifier.height(12.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            if (isPersistent) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Outlined.PushPin,
                                                    contentDescription = "Persistent mode active",
                                                    modifier = Modifier.size(10.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                        keyboardContent()
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
                } else {
                    keyboardContent()
                }
            }
        }
    }
}
