package mega.privacy.android.feature.sync.ui.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import mega.android.core.ui.components.dialogs.BasicDialogButton
import mega.android.core.ui.components.dialogs.BasicDialogRadioOption
import mega.android.core.ui.components.dialogs.BasicRadioDialog
import mega.android.core.ui.components.text.SpannableText
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.feature.sync.ui.model.SyncPowerOption
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
    val resources = LocalResources.current
    val options = SyncPowerOption.entries
        .map { BasicDialogRadioOption(ordinal = it.ordinal, text = resources.getString(it.labelId)) }
        .toImmutableList()

    BasicRadioDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = SpannableText(stringResource(sharedRes.string.settings_sync_battery_usage_title)),
        description = SpannableText(stringResource(sharedRes.string.settings_sync_battery_usage_description)),
        options = options,
        selectedOption = options.firstOrNull { it.ordinal == selectedOption.ordinal },
        onOptionSelected = { option ->
            val selected = SyncPowerOption.entries.first { it.ordinal == option.ordinal }
            onSyncPowerOptionsClicked(selected)
            when (selected) {
                SyncPowerOption.SyncAlways -> Analytics.tracker.trackEvent(
                    SyncPowerOptionSelectedEvent(SyncPowerOptionSelected.SelectionType.SyncAlways)
                )

                SyncPowerOption.SyncOnlyWhenCharging -> Analytics.tracker.trackEvent(
                    SyncPowerOptionSelectedEvent(SyncPowerOptionSelected.SelectionType.SyncOnlyWhenCharging)
                )
            }
        },
        buttons = persistentListOf(
            BasicDialogButton(
                text = stringResource(sharedRes.string.general_dialog_cancel_button),
                onClick = onDismiss,
            )
        ),
    )
}

private val SyncPowerOption.labelId: Int
    get() = when (this) {
        SyncPowerOption.SyncAlways -> sharedRes.string.settings_sync_power_always_title
        SyncPowerOption.SyncOnlyWhenCharging -> sharedRes.string.settings_sync_battery_sync_only_when_charging_title
    }

@CombinedThemePreviews
@Composable
private fun SyncPowerOptionsDialogPreview() {
    AndroidThemeForPreviews {
        SyncPowerOptionsDialog(
            onDismiss = {},
            onSyncPowerOptionsClicked = {},
            selectedOption = SyncPowerOption.SyncAlways
        )
    }
}
