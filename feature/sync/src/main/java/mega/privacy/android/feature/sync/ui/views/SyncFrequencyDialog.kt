package mega.privacy.android.feature.sync.ui.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import mega.android.core.ui.components.dialogs.BasicDialogButton
import mega.android.core.ui.components.dialogs.BasicDialogRadioOption
import mega.android.core.ui.components.dialogs.BasicRadioDialog
import mega.android.core.ui.components.text.SpannableText
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.sync.ui.model.SyncFrequency
import mega.privacy.android.shared.resources.R as sharedRes

@Composable
internal fun SyncFrequencyDialog(
    onDismiss: () -> Unit,
    onSyncFrequencyClicked: (SyncFrequency) -> Unit,
    selectedSyncFrequency: SyncFrequency,
    modifier: Modifier = Modifier,
) {
    val options = SyncFrequency.entries
        .map { BasicDialogRadioOption(ordinal = it.ordinal, text = frequencyToString(it)) }
        .toImmutableList()

    BasicRadioDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = SpannableText("Sync frequency"),
        options = options,
        selectedOption = options.firstOrNull { it.ordinal == selectedSyncFrequency.ordinal },
        onOptionSelected = { option ->
            onSyncFrequencyClicked(SyncFrequency.entries.first { it.ordinal == option.ordinal })
        },
        buttons = persistentListOf(
            BasicDialogButton(
                text = stringResource(sharedRes.string.general_dialog_cancel_button),
                onClick = onDismiss,
            )
        ),
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
private fun SyncFrequencyDialogPreview() {
    AndroidThemeForPreviews {
        SyncFrequencyDialog(
            onDismiss = {},
            onSyncFrequencyClicked = {},
            selectedSyncFrequency = SyncFrequency.EVERY_15_MINUTES
        )
    }
}
