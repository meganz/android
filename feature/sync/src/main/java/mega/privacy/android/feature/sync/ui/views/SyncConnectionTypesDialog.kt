package mega.privacy.android.feature.sync.ui.views

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.ui.model.SyncConnectionType
import mega.privacy.android.shared.original.core.ui.controls.dialogs.ConfirmationDialogWithRadioButtons
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R as sharedRes
import mega.privacy.mobile.analytics.event.SyncOptionSelected
import mega.privacy.mobile.analytics.event.SyncOptionSelectedEvent

@Composable
internal fun SyncConnectionTypesDialog(
    onDismiss: () -> Unit,
    onSyncNetworkOptionsClicked: (SyncConnectionType) -> Unit,
    selectedOption: SyncConnectionType,
    modifier: Modifier = Modifier,
) {
    ConfirmationDialogWithRadioButtons(
        radioOptions = listOf(
            SyncConnectionType.WiFiOrMobileData,
            SyncConnectionType.WiFiOnly,
        ),
        onOptionSelected = {
            onSyncNetworkOptionsClicked(it)
            when (it) {
                SyncConnectionType.WiFiOrMobileData -> {
                    Analytics.tracker.trackEvent(
                        SyncOptionSelectedEvent(SyncOptionSelected.SelectionType.SyncOptionWifiAndMobileSelected)
                    )
                }

                SyncConnectionType.WiFiOnly -> {
                    Analytics.tracker.trackEvent(
                        SyncOptionSelectedEvent(SyncOptionSelected.SelectionType.SyncOptionWifiOnlySelected)
                    )
                }
            }
        },
        initialSelectedOption = selectedOption,
        onDismissRequest = onDismiss,
        cancelButtonText = stringResource(sharedRes.string.general_dialog_cancel_button),
        optionDescriptionMapper = { syncNetworkOption ->
            when (syncNetworkOption) {
                SyncConnectionType.WiFiOrMobileData -> stringResource(
                    id = R.string.sync_dialog_message_wifi_or_mobile_data
                )

                SyncConnectionType.WiFiOnly -> stringResource(
                    id = R.string.sync_dialog_message_wifi_only
                )
            }
        },
        titleText = stringResource(sharedRes.string.settings_sync_connection_type_title),
        modifier = modifier
    )
}

@CombinedThemePreviews
@Composable
private fun SyncNetworkOptionsDialogPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        SyncConnectionTypesDialog(
            onDismiss = {},
            onSyncNetworkOptionsClicked = {},
            selectedOption = SyncConnectionType.WiFiOrMobileData
        )
    }
}
