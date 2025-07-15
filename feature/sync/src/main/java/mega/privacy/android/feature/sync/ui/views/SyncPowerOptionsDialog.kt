package mega.privacy.android.feature.sync.ui.views

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.feature.sync.ui.model.SyncPowerOption
import mega.privacy.android.shared.original.core.ui.controls.dialogs.ConfirmationDialogWithRadioButtons
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R as sharedRes
import mega.privacy.mobile.analytics.event.SyncPowerOptionSelected
import mega.privacy.mobile.analytics.event.SyncPowerOptionSelectedEvent

@Composable
internal fun SyncPowerOptionsDialog(
    onDismiss: () -> Unit,
    onSyncPowerOptionsClicked: (SyncPowerOption) -> Unit,
    selectedOption: SyncPowerOption,
    modifier: Modifier = Modifier,
) {
    ConfirmationDialogWithRadioButtons(
        radioOptions = listOf(
            SyncPowerOption.SyncAlways,
            SyncPowerOption.SyncOnlyWhenCharging,
        ),
        onOptionSelected = {
            onSyncPowerOptionsClicked(it)
            when (it) {
                SyncPowerOption.SyncAlways -> {
                    Analytics.tracker.trackEvent(
                        SyncPowerOptionSelectedEvent(SyncPowerOptionSelected.SelectionType.SyncAlways)
                    )
                }

                SyncPowerOption.SyncOnlyWhenCharging -> {
                    Analytics.tracker.trackEvent(
                        SyncPowerOptionSelectedEvent(SyncPowerOptionSelected.SelectionType.SyncOnlyWhenCharging)
                    )
                }
            }
        },
        initialSelectedOption = selectedOption,
        onDismissRequest = onDismiss,
        cancelButtonText = stringResource(sharedRes.string.general_dialog_cancel_button),
        optionDescriptionMapper = { syncPowerOption ->
            when (syncPowerOption) {
                SyncPowerOption.SyncAlways -> stringResource(
                    id = sharedRes.string.settings_sync_power_always_title
                )

                SyncPowerOption.SyncOnlyWhenCharging -> stringResource(
                    id = sharedRes.string.settings_sync_battery_sync_only_when_charging_title
                )
            }
        },
        titleText = stringResource(sharedRes.string.settings_sync_power_settings_title),
        modifier = modifier
    )
}

@CombinedThemePreviews
@Composable
private fun SyncPowerOptionsDialogPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        SyncPowerOptionsDialog(
            onDismiss = {},
            onSyncPowerOptionsClicked = {},
            selectedOption = SyncPowerOption.SyncAlways
        )
    }
}
