package com.dessalines.thumbkey.ui.components.settings.floatingmode

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Opacity
import androidx.compose.material.icons.outlined.PictureInPicture
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.navigation.NavController
import com.dessalines.thumbkey.R
import com.dessalines.thumbkey.db.FloatingModeConfig
import com.dessalines.thumbkey.db.FloatingModeViewModel
import com.dessalines.thumbkey.utils.SimpleTopAppBar
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.ListPreferenceType
import me.zhanghai.compose.preference.Preference
import me.zhanghai.compose.preference.ProvidePreferenceTheme
import me.zhanghai.compose.preference.SliderPreference
import me.zhanghai.compose.preference.SwitchPreference

enum class FloatingPreset(val labelResId: Int) {
    BOTTOM(R.string.floating_preset_bottom),
    TOP_LEFT(R.string.floating_preset_topleft),
    CENTER(R.string.floating_preset_center),
    RIGHT(R.string.floating_preset_right)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingModeScreen(
    navController: NavController,
    floatingModeViewModel: FloatingModeViewModel,
) {
    val ctx = LocalContext.current
    val settingsState = floatingModeViewModel.config.observeAsState()
    val settings = settingsState.value

    val enabled = settings?.enabled == 1
    val opacity = settings?.opacity ?: 100

    var opacitySliderState by remember(opacity) { mutableFloatStateOf(opacity.toFloat()) }

    val displayMetrics = ctx.resources.displayMetrics
    val screenWidth = displayMetrics.widthPixels
    val screenHeight = displayMetrics.heightPixels

    val currentPreset = when {
        settings?.enabled == 0 -> FloatingPreset.BOTTOM
        (settings?.x ?: 0) <= 100 && (settings?.y ?: 0) <= 200 -> FloatingPreset.TOP_LEFT
        (settings?.x ?: 0) >= screenWidth / 2 -> FloatingPreset.RIGHT
        else -> FloatingPreset.CENTER
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SimpleTopAppBar(
                text = stringResource(R.string.floating_mode),
                navController = navController,
            )
        },
        content = { padding ->
            Column(
                modifier =
                    Modifier
                        .padding(padding)
                        .verticalScroll(scrollState)
                        .background(color = MaterialTheme.colorScheme.surface)
                        .imePadding(),
            ) {
                ProvidePreferenceTheme {
                    SwitchPreference(
                        value = enabled,
                        onValueChange = { newValue ->
                            if (newValue && !Settings.canDrawOverlays(ctx)) {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${ctx.packageName}")
                                )
                                ctx.startActivity(intent)
                            } else {
                                floatingModeViewModel.update(
                                    (settings ?: FloatingModeConfig()).copy(
                                        enabled = if (newValue) 1 else 0
                                    )
                                )
                            }
                        },
                        title = { Text(stringResource(R.string.floating_mode_enable)) },
                        summary = { Text(stringResource(R.string.floating_mode_enable_description)) },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.PictureInPicture,
                                contentDescription = null,
                            )
                        },
                    )

                    SwitchPreference(
                        value = settings?.expandInFloating == 1,
                        onValueChange = { newValue ->
                            floatingModeViewModel.update(
                                (settings ?: FloatingModeConfig()).copy(
                                    expandInFloating = if (newValue) 1 else 0
                                )
                            )
                        },
                        title = { Text(stringResource(R.string.floating_expand_in_floating)) },
                        summary = { Text(stringResource(R.string.floating_expand_in_floating_description)) },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.PictureInPicture,
                                contentDescription = null,
                            )
                        },
                        enabled = enabled,
                    )

                    val maxWidthFraction = settings?.maxWidthFraction ?: 60
                    var widthSliderState by remember(maxWidthFraction) { mutableFloatStateOf(maxWidthFraction.toFloat()) }

                    SliderPreference(
                        value = maxWidthFraction.toFloat(),
                        sliderValue = widthSliderState,
                        onValueChange = {
                            floatingModeViewModel.update(
                                (settings ?: FloatingModeConfig()).copy(
                                    maxWidthFraction = it.toInt()
                                )
                            )
                        },
                        onSliderValueChange = { widthSliderState = it },
                        valueRange = 30f..100f,
                        title = {
                            Text(stringResource(R.string.floating_max_width, widthSliderState.toInt()))
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Dashboard,
                                contentDescription = null,
                            )
                        },
                        enabled = enabled && settings?.expandInFloating == 1,
                    )

                    SliderPreference(
                        value = opacity.toFloat(),
                        sliderValue = opacitySliderState,
                        onValueChange = {
                            floatingModeViewModel.update(
                                (settings ?: FloatingModeConfig()).copy(
                                    opacity = it.toInt()
                                )
                            )
                        },
                        onSliderValueChange = { opacitySliderState = it },
                        valueRange = 50f..100f,
                        title = {
                            Text(stringResource(R.string.floating_opacity, opacitySliderState.toInt()))
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Opacity,
                                contentDescription = null,
                            )
                        },
                        enabled = enabled,
                    )

                    ListPreference(
                        type = ListPreferenceType.DROPDOWN_MENU,
                        value = currentPreset,
                        values = FloatingPreset.entries,
                        valueToText = {
                            AnnotatedString(ctx.getString(it.labelResId))
                        },
                        onValueChange = { preset ->
                            val current = settings ?: FloatingModeConfig()
                            val updated = when (preset) {
                                FloatingPreset.BOTTOM -> current.copy(enabled = 0)
                                FloatingPreset.TOP_LEFT -> current.copy(enabled = 1, x = 50, y = 150)
                                FloatingPreset.CENTER -> current.copy(enabled = 1, x = screenWidth / 6, y = screenHeight / 3)
                                FloatingPreset.RIGHT -> current.copy(enabled = 1, x = screenWidth / 3, y = screenHeight / 2)
                            }

                            if (preset != FloatingPreset.BOTTOM && !Settings.canDrawOverlays(ctx)) {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${ctx.packageName}")
                                )
                                ctx.startActivity(intent)
                            } else {
                                floatingModeViewModel.update(updated)
                            }
                        },
                        title = { Text(stringResource(R.string.floating_presets)) },
                        summary = { Text(stringResource(currentPreset.labelResId)) },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Dashboard,
                                contentDescription = null,
                            )
                        },
                    )

                    Preference(
                        title = { Text(stringResource(R.string.reset_to_defaults)) },
                        onClick = {
                            floatingModeViewModel.update(FloatingModeConfig())
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.RestartAlt,
                                contentDescription = null,
                            )
                        },
                    )
                }
            }
        },
    )
}
