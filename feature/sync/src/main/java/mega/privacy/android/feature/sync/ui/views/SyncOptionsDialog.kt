package mega.privacy.android.feature.sync.ui.views

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.controls.dialogs.ConfirmationDialogWithRadioButtons
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.feature.sync.R
import mega.privacy.android.shared.resources.R as sharedRes
import mega.privacy.android.feature.sync.ui.model.SyncOption

@Composable
internal fun SyncOptionsDialog(
    onDismiss: () -> Unit,
    onSyncOptionsClicked: (SyncOption) -> Unit,
    selectedOption: SyncOption,
    modifier: Modifier = Modifier,
) {
    ConfirmationDialogWithRadioButtons(
        radioOptions = listOf(
            SyncOption.WI_FI_OR_MOBILE_DATA,
            SyncOption.WI_FI_ONLY
        ),
        onOptionSelected = {
            onSyncOptionsClicked(it)
        },
        initialSelectedOption = selectedOption,
        onDismissRequest = onDismiss,
        cancelButtonText = stringResource(sharedRes.string.settings_sync_option_cancel),
        optionDescriptionMapper = { syncOption ->
            when (syncOption) {
                SyncOption.WI_FI_OR_MOBILE_DATA -> stringResource(
                    id = R.string.sync_dialog_message_wifi_or_mobile_data
                )

                SyncOption.WI_FI_ONLY -> stringResource(
                    id = R.string.sync_dialog_message_wifi_only
                )
            }
        },
        titleText = "Sync options",
        modifier = modifier
    )
}

@CombinedThemePreviews
@Composable
private fun SyncOptionsDialogPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        SyncOptionsDialog(
            onDismiss = {},
            onSyncOptionsClicked = {},
            selectedOption = SyncOption.WI_FI_OR_MOBILE_DATA
        )
    }
}