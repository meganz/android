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
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.ui.model.SyncConnectionType
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
    val resources = LocalResources.current
    val options = SyncConnectionType.entries
        .map { BasicDialogRadioOption(ordinal = it.ordinal, text = resources.getString(it.labelId)) }
        .toImmutableList()

    BasicRadioDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = SpannableText(stringResource(sharedRes.string.settings_sync_connection_type_title)),
        options = options,
        selectedOption = options.firstOrNull { it.ordinal == selectedOption.ordinal },
        onOptionSelected = { option ->
            val selected = SyncConnectionType.entries.first { it.ordinal == option.ordinal }
            onSyncNetworkOptionsClicked(selected)
            when (selected) {
                SyncConnectionType.WiFiOrMobileData -> Analytics.tracker.trackEvent(
                    SyncOptionSelectedEvent(SyncOptionSelected.SelectionType.SyncOptionWifiAndMobileSelected)
                )

                SyncConnectionType.WiFiOnly -> Analytics.tracker.trackEvent(
                    SyncOptionSelectedEvent(SyncOptionSelected.SelectionType.SyncOptionWifiOnlySelected)
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

private val SyncConnectionType.labelId: Int
    get() = when (this) {
        SyncConnectionType.WiFiOnly -> R.string.sync_dialog_message_wifi_only
        SyncConnectionType.WiFiOrMobileData -> R.string.sync_dialog_message_wifi_or_mobile_data
    }

@CombinedThemePreviews
@Composable
private fun SyncConnectionTypesDialogPreview() {
    AndroidThemeForPreviews {
        SyncConnectionTypesDialog(
            onDismiss = {},
            onSyncNetworkOptionsClicked = {},
            selectedOption = SyncConnectionType.WiFiOrMobileData
        )
    }
}
