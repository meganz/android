package mega.privacy.android.feature.sync.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import mega.android.core.ui.components.list.FlexibleLineListItem
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.ui.model.SyncConnectionType
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.resources.R as sharedR

@Composable
internal fun SyncConnectionTypeView(
    syncNetworkOption: SyncConnectionType,
    modifier: Modifier = Modifier,
    syncConnectionTypeClicked: () -> Unit,
) {
    FlexibleLineListItem(
        modifier = modifier.testTag(SETTINGS_SYNC_CONNECTION_TYPE_VIEW),
        title = stringResource(sharedR.string.settings_sync_connection_type_title),
        subtitle = when (syncNetworkOption) {
            SyncConnectionType.WiFiOnly -> stringResource(R.string.sync_dialog_message_wifi_only)
            SyncConnectionType.WiFiOrMobileData -> stringResource(R.string.sync_dialog_message_wifi_or_mobile_data)
        },
        enableClick = true,
        onClickListener = syncConnectionTypeClicked,
    )
}

@Composable
@CombinedThemePreviews
private fun SyncNetworkOptionsViewPreview(
    @PreviewParameter(BooleanProvider::class) syncOnlyByWifi: Boolean,
) {
    AndroidThemeForPreviews {
        SyncConnectionTypeView(
            syncNetworkOption = if (syncOnlyByWifi) SyncConnectionType.WiFiOnly else SyncConnectionType.WiFiOrMobileData,
            syncConnectionTypeClicked = {},
        )
    }
}

private const val SETTINGS_SYNC_CONNECTION_TYPE_VIEW = "SETTINGS_SYNC_CONNECTION_TYPE_VIEW"
