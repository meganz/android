package mega.privacy.android.app.presentation.slideshow.view

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.slideshow.SlideshowSettingViewModel
import mega.privacy.android.core.ui.controls.controlssliders.LabelledSwitch
import mega.privacy.android.core.ui.controls.dialogs.MegaDialog
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.black
import mega.privacy.android.core.ui.theme.button
import mega.privacy.android.core.ui.theme.grey_alpha_012
import mega.privacy.android.core.ui.theme.grey_alpha_054
import mega.privacy.android.core.ui.theme.grey_alpha_087
import mega.privacy.android.core.ui.theme.teal_200
import mega.privacy.android.core.ui.theme.teal_300
import mega.privacy.android.core.ui.theme.white
import mega.privacy.android.core.ui.theme.white_alpha_012
import mega.privacy.android.core.ui.theme.white_alpha_054
import mega.privacy.android.core.ui.theme.white_alpha_087
import mega.privacy.android.domain.entity.slideshow.SlideshowOrder
import mega.privacy.android.domain.entity.slideshow.SlideshowSpeed
import mega.privacy.mobile.analytics.event.SlideshowSettingOrderNewestButtonEvent
import mega.privacy.mobile.analytics.event.SlideshowSettingOrderOldestButtonEvent
import mega.privacy.mobile.analytics.event.SlideshowSettingOrderShuffleButtonEvent
import mega.privacy.mobile.analytics.event.SlideshowSettingRepeatOffButtonEvent
import mega.privacy.mobile.analytics.event.SlideshowSettingRepeatOnButtonEvent
import mega.privacy.mobile.analytics.event.SlideshowSettingSpeedFastButtonEvent
import mega.privacy.mobile.analytics.event.SlideshowSettingSpeedNormalButtonEvent
import mega.privacy.mobile.analytics.event.SlideshowSettingSpeedSlowButtonEvent

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun SlideshowSettingsView(
    slideshowSettingViewModel: SlideshowSettingViewModel,
) {
    val state by slideshowSettingViewModel.state.collectAsStateWithLifecycle()
    val isLight = MaterialTheme.colors.isLight
    val settingsTitleFontSize = 16.sp
    val settingsSubtitleFontSize = 14.sp
    val settingsTitleColour = black.takeIf { isLight } ?: white
    val settingsSubtitleColour = grey_alpha_054.takeIf { isLight } ?: white_alpha_054
    val settingsDividerColour = grey_alpha_012.takeIf { isLight } ?: white_alpha_012

    var openSpeedDialog by rememberSaveable { mutableStateOf(false) }
    var openOrderDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 100.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp)
                .clickable { openSpeedDialog = true },
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = stringResource(id = R.string.slideshow_setting_speed),
                fontSize = settingsTitleFontSize,
                color = settingsTitleColour,
            )
            Text(
                text = state.speed?.displayName().orEmpty(),
                color = settingsSubtitleColour,
                fontSize = settingsSubtitleFontSize
            )
        }

        Divider(color = settingsDividerColour, thickness = 1.dp)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp)
                .clickable { openOrderDialog = true },
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = stringResource(id = R.string.slideshow_settings_order),
                fontSize = settingsTitleFontSize,
                color = settingsTitleColour,
            )
            Text(
                text = state.order?.displayName().orEmpty(),
                color = settingsSubtitleColour,
                fontSize = settingsSubtitleFontSize
            )
        }

        Divider(color = settingsDividerColour, thickness = 1.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LabelledSwitch(
                label = stringResource(R.string.slideshow_setting_repeat),
                checked = state.repeat,
                onCheckChanged = {
                    slideshowRepeatAnalytics(!state.repeat)
                    slideshowSettingViewModel.saveRepeatSetting(!state.repeat)
                },
                modifier = Modifier
                    .fillMaxWidth(),
            )
        }

        Divider(color = settingsDividerColour, thickness = 1.dp)

        if (openSpeedDialog) {
            SlideshowSpeedDialog(
                onDialogDismissed = { openSpeedDialog = false },
                options = SlideshowSpeed.values(),
                selectedOption = state.speed ?: SlideshowSpeed.Normal,
            ) {
                slideshowSpeedAnalytics(it)
                slideshowSettingViewModel.saveSpeedSetting(it)
            }
        }

        if (openOrderDialog) {
            SlideshowOrderDialog(
                onDialogDismissed = { openOrderDialog = false },
                options = SlideshowOrder.values(),
                selectedOption = state.order ?: SlideshowOrder.Shuffle,
            ) {
                slideshowOrderAnalytics(it)
                slideshowSettingViewModel.saveOrderSetting(it)
            }
        }
    }
}

@Composable
private fun SlideshowSpeedDialog(
    onDialogDismissed: () -> Unit = {},
    options: Array<SlideshowSpeed> = arrayOf(),
    selectedOption: SlideshowSpeed? = null,
    onOptionSelected: (SlideshowSpeed) -> Unit = {},
) {
    val isLight = MaterialTheme.colors.isLight

    MegaDialog(
        titleString = stringResource(id = R.string.slideshow_setting_speed),
        fontWeight = FontWeight(500),
        onDismissRequest = onDialogDismissed,
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDialogDismissed,
            ) {
                Text(
                    text = stringResource(id = R.string.button_cancel),
                    style = button,
                    color = teal_300.takeIf { isLight } ?: teal_200
                )
            }
        },
        body = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.size(12.dp))
                options.forEach { option ->
                    val isSelected = option == selectedOption
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOptionSelected(option)
                                onDialogDismissed()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = null,
                        )
                        Spacer(modifier = Modifier.size(16.dp))
                        Text(
                            text = option.displayName(),
                            color = if (isLight) {
                                grey_alpha_087.takeIf { isSelected } ?: grey_alpha_054
                            } else {
                                white_alpha_087.takeIf { isSelected } ?: white_alpha_054
                            },
                            style = MaterialTheme.typography.body1,
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun SlideshowOrderDialog(
    onDialogDismissed: () -> Unit = {},
    options: Array<SlideshowOrder> = arrayOf(),
    selectedOption: SlideshowOrder? = null,
    onOptionSelected: (SlideshowOrder) -> Unit = {},
) {
    val isLight = MaterialTheme.colors.isLight

    MegaDialog(
        titleString = stringResource(id = R.string.slideshow_settings_order),
        onDismissRequest = onDialogDismissed,
        fontWeight = FontWeight(500),
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDialogDismissed,
            ) {
                Text(
                    text = stringResource(id = R.string.button_cancel),
                    style = button,
                    color = teal_300.takeIf { isLight } ?: teal_200
                )
            }
        },
        body = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.size(12.dp))
                options.forEach { option ->
                    val isSelected = option == selectedOption
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOptionSelected(option)
                                onDialogDismissed()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = null,
                        )
                        Spacer(modifier = Modifier.size(16.dp))
                        Text(
                            text = option.displayName(),
                            color = if (isLight) {
                                grey_alpha_087.takeIf { isSelected } ?: grey_alpha_054
                            } else {
                                white_alpha_087.takeIf { isSelected } ?: white_alpha_054
                            },
                            style = MaterialTheme.typography.body1,
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun SlideshowOrder.displayName(): String = when (this) {
    SlideshowOrder.Shuffle -> stringResource(id = R.string.slideshow_order_shuffle)
    SlideshowOrder.Newest -> stringResource(id = R.string.sortby_date_newest)
    SlideshowOrder.Oldest -> stringResource(id = R.string.sortby_date_oldest)
}

@Composable
private fun SlideshowSpeed.displayName(): String = when (this) {
    SlideshowSpeed.Slow -> stringResource(id = R.string.slideshow_speed_slow)
    SlideshowSpeed.Normal -> stringResource(id = R.string.slideshow_speed_normal)
    SlideshowSpeed.Fast -> stringResource(id = R.string.slideshow_speed_fast)
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "DarkPreviewSlideshowSettings"
)
@Preview
@Composable
fun PreviewSlideshowSettingsView() {
    var selectedSpeed by remember { mutableStateOf("Normal (4s)") }
    var selectedOrder by remember { mutableStateOf("Shuffle") }
    var repeat by remember { mutableStateOf(false) }
    AndroidTheme(isSystemInDarkTheme()) {
        Scaffold {
            SlideshowSettingsView(viewModel())
        }
    }
}

private fun slideshowSpeedAnalytics(speed: SlideshowSpeed) {
    when (speed) {
        SlideshowSpeed.Slow -> Analytics.tracker.trackEvent(
            SlideshowSettingSpeedSlowButtonEvent
        )

        SlideshowSpeed.Normal -> Analytics.tracker.trackEvent(
            SlideshowSettingSpeedNormalButtonEvent
        )

        SlideshowSpeed.Fast -> Analytics.tracker.trackEvent(
            SlideshowSettingSpeedFastButtonEvent
        )
    }
}

private fun slideshowOrderAnalytics(order: SlideshowOrder) {
    when (order) {
        SlideshowOrder.Newest -> Analytics.tracker.trackEvent(
            SlideshowSettingOrderNewestButtonEvent
        )

        SlideshowOrder.Oldest -> Analytics.tracker.trackEvent(
            SlideshowSettingOrderOldestButtonEvent
        )

        SlideshowOrder.Shuffle -> Analytics.tracker.trackEvent(
            SlideshowSettingOrderShuffleButtonEvent
        )
    }
}

private fun slideshowRepeatAnalytics(repeat: Boolean) {
    if (repeat) Analytics.tracker.trackEvent(SlideshowSettingRepeatOnButtonEvent)
    else Analytics.tracker.trackEvent(SlideshowSettingRepeatOffButtonEvent)
}