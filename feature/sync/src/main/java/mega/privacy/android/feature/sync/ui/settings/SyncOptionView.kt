package mega.privacy.android.feature.sync.ui.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.ui.model.SyncOption
import mega.privacy.android.shared.original.core.ui.controls.lists.GenericTwoLineListItem
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

@Composable
internal fun SyncOptionView(
    syncOption: SyncOption,
    modifier: Modifier = Modifier,
    syncOptionsClicked: () -> Unit,
) {
    GenericTwoLineListItem(
        modifier = modifier.testTag(SETTINGS_SYNC_SYNC_OPTIONS_VIEW),
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

@Composable
@CombinedThemePreviews
private fun SyncOptionsViewPreview(
    @PreviewParameter(BooleanProvider::class) syncOnlyByWifi: Boolean,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        SyncOptionView(
            syncOption = if (syncOnlyByWifi) SyncOption.WI_FI_ONLY else SyncOption.WI_FI_OR_MOBILE_DATA,
            syncOptionsClicked = {},
        )
    }
}

private const val SETTINGS_SYNC_SYNC_OPTIONS_VIEW = "SETTINGS_SYNC_OPTIONS_VIEW"