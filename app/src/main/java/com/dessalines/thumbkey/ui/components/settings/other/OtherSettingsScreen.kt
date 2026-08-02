package com.dessalines.thumbkey.ui.components.settings.other

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitScreen
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Opacity
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dessalines.thumbkey.R
import com.dessalines.thumbkey.ThumbkeyApplication
import com.dessalines.thumbkey.db.AppSettingsViewModel
import com.dessalines.thumbkey.db.DEFAULT_FLOATING_MODE_ENABLED
import com.dessalines.thumbkey.db.DEFAULT_SHOW_ON_SCREEN_KEYBOARD
import com.dessalines.thumbkey.db.OtherSettingsUpdate
import com.dessalines.thumbkey.utils.SimpleTopAppBar
import com.dessalines.thumbkey.utils.TAG
import com.dessalines.thumbkey.utils.toBool
import com.dessalines.thumbkey.utils.toInt
import me.zhanghai.compose.preference.ProvidePreferenceTheme
import me.zhanghai.compose.preference.SwitchPreference

const val DEFAULT_FLOATING_EXPAND_IN_FLOATING = 0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherSettingsScreen(
    navController: NavController,
    appSettingsViewModel: AppSettingsViewModel,
) {
    Log.d(TAG, "Got to 'other' settings activity")

    val settings by appSettingsViewModel.appSettings.observeAsState()

    var showOnScreenKeyboardState =
        (settings?.showOnScreenKeyboard ?: DEFAULT_SHOW_ON_SCREEN_KEYBOARD).toBool()
    var floatingModeEnabledState =
        (settings?.floatingModeEnabled ?: DEFAULT_FLOATING_MODE_ENABLED).toBool()

    val context = LocalContext.current
    val app = context.applicationContext as ThumbkeyApplication
    var opacityValue by remember { mutableFloatStateOf(app.floatingModeRepository.getOpacity().toFloat()) }
    var expandInFloating by remember { mutableStateOf(app.floatingModeRepository.isExpandInFloating()) }
    var maxWidthFraction by remember { mutableFloatStateOf(app.floatingModeRepository.getMaxWidthFraction().toFloat()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    fun updateOtherSettings() {
        appSettingsViewModel.updateOtherSettings(
            OtherSettingsUpdate(
                id = 1,
                showOnScreenKeyboard = showOnScreenKeyboardState.toInt(),
                floatingModeEnabled = floatingModeEnabledState.toInt(),
            ),
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SimpleTopAppBar(
                text = stringResource(R.string.other),
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
                        value = showOnScreenKeyboardState,
                        onValueChange = {
                            showOnScreenKeyboardState = it
                            updateOtherSettings()
                        },
                        title = {
                            Text(stringResource(R.string.show_on_screen_keyboard))
                        },
                        summary = {
                            Text(stringResource(R.string.show_on_screen_keyboard_description))
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Keyboard,
                                contentDescription = null,
                            )
                        },
                    )
                    SwitchPreference(
                        value = floatingModeEnabledState,
                        onValueChange = {
                            floatingModeEnabledState = it
                            updateOtherSettings()
                        },
                        title = {
                            Text(stringResource(R.string.floating_mode))
                        },
                        summary = {
                            Text(stringResource(R.string.floating_mode_description))
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.FitScreen,
                                contentDescription = null,
                            )
                        },
                    )
                    if (floatingModeEnabledState) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                            verticalAlignment =
                                androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Opacity,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "${opacityValue.toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Slider(
                                value = opacityValue,
                                onValueChange = { opacityValue = it },
                                onValueChangeFinished = {
                                    app.floatingModeRepository.saveOpacity(
                                        opacityValue.toInt().coerceIn(30, 100),
                                    )
                                },
                                valueRange = 30f..100f,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        SwitchPreference(
                            value = expandInFloating,
                            onValueChange = {
                                expandInFloating = it
                                app.floatingModeRepository.setExpandInFloating(it)
                            },
                            title = {
                                Text(stringResource(R.string.floating_expand_in_floating))
                            },
                            summary = {
                                Text(stringResource(R.string.floating_expand_in_floating_description))
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.OpenInFull,
                                    contentDescription = null,
                                )
                            },
                        )
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                            verticalAlignment =
                                androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FitScreen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "${maxWidthFraction.toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Slider(
                                value = maxWidthFraction,
                                onValueChange = { maxWidthFraction = it },
                                onValueChangeFinished = {
                                    app.floatingModeRepository.setMaxWidthFraction(
                                        maxWidthFraction.toInt().coerceIn(30, 100),
                                    )
                                },
                                valueRange = 30f..100f,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        },
    )
}
