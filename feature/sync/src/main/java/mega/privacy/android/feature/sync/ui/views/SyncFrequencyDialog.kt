package mega.privacy.android.feature.sync.ui.views

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import mega.privacy.android.feature.sync.ui.model.SyncFrequency
import mega.privacy.android.shared.original.core.ui.controls.dialogs.ConfirmationDialogWithRadioButtons
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.resources.R as sharedRes

@Composable
internal fun SyncFrequencyDialog(
    onDismiss: () -> Unit,
    onSyncFrequencyClicked: (SyncFrequency) -> Unit,
    selectedSyncFrequency: SyncFrequency,
    modifier: Modifier = Modifier,
) {
    ConfirmationDialogWithRadioButtons(
        radioOptions = SyncFrequency.entries,
        onOptionSelected = {
            onSyncFrequencyClicked(it)
        },
        initialSelectedOption = selectedSyncFrequency,
        cancelButtonText = stringResource(sharedRes.string.settings_sync_option_cancel),
        onDismissRequest = onDismiss,
        optionDescriptionMapper = { syncFrequency ->
            frequencyToString(syncFrequency)
        },
        titleText = "Sync frequency",
        modifier = modifier
    )
}

private fun frequencyToString(syncFrequency: SyncFrequency): String {
    return when (syncFrequency) {
        SyncFrequency.EVERY_15_MINUTES -> "15 minutes"
        SyncFrequency.EVERY_30_MINUTES -> "30 minutes"
        SyncFrequency.EVERY_45_MINUTES -> "45 minutes"
        SyncFrequency.EVERY_HOUR -> "1 hour"
    }
}

@CombinedThemePreviews
@Composable
private fun SyncOptionsDialogPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        SyncFrequencyDialog(
            onDismiss = {},
            onSyncFrequencyClicked = {},
            selectedSyncFrequency = SyncFrequency.EVERY_15_MINUTES,
        )
    }
}