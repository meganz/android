package mega.privacy.android.feature.sync.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.ui.model.SyncOption
import mega.privacy.android.shared.original.core.ui.controls.lists.GenericTwoLineListItem

@Composable
internal fun SyncOptionView(
    syncOption: SyncOption,
    syncOptionsClicked: () -> Unit,
) {
    GenericTwoLineListItem(
        modifier = Modifier.testTag(SETTINGS_SYNC_SYNC_OPTIONS_VIEW),
        title = "Sync options",
        subtitle = if (syncOption == SyncOption.WI_FI_ONLY) {
            stringResource(R.string.sync_dialog_message_wifi_only)
        } else {
            stringResource(R.string.sync_dialog_message_wifi_or_mobile_data)
        },
        showEntireSubtitle = true,
        onItemClicked = syncOptionsClicked,
    )
}

private const val SETTINGS_SYNC_SYNC_OPTIONS_VIEW = "SETTINGS_SYNC_OPTIONS_VIEW"