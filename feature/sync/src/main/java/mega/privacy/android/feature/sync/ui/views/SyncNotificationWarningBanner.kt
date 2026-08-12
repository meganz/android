package mega.privacy.android.feature.sync.ui.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import mega.android.core.ui.components.banner.InlineErrorBanner
import mega.android.core.ui.components.banner.InlineWarningBanner
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationType
import mega.privacy.android.feature.sync.ui.SyncMonitorState
import mega.privacy.android.shared.resources.R as sharedResR


/**
 * Banner to show sync notification warning
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal fun SyncNotificationWarningBanner(
    state: SyncMonitorState,
    onDismissNotification: () -> Unit,
    modifier: Modifier = Modifier,
) {
    state.displayNotification?.let {
        if (it.syncNotificationType == SyncNotificationType.NOT_CONNECTED_TO_WIFI) {
            InlineWarningBanner(
                modifier = modifier,
                title = stringResource(sharedResR.string.general_sync_warning_lost_wifi_title),
                body = stringResource(sharedResR.string.general_sync_warning_lost_wifi_text),
                showCancelButton = true,
                onCancelButtonClick = {
                    onDismissNotification()
                },
            )
        }
        if (it.syncNotificationType == SyncNotificationType.CHANGE_SYNC_ROOT) {
            InlineErrorBanner(
                title = stringResource(sharedResR.string.sync_change_root_uri_warning_title),
                body = null,
                showCancelButton = false,
            )
        }
    }
}
