package mega.privacy.android.feature.sync.ui.views

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationType
import mega.privacy.android.feature.sync.ui.SyncMonitorState
import mega.privacy.android.shared.original.core.ui.controls.banners.InlineWarningBanner
import mega.privacy.android.shared.original.core.ui.theme.extensions.body3


/**
 * Banner to show sync notification warning
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SyncNotificationWarningBanner(
    state: SyncMonitorState,
    onDismissNotification: () -> Unit,
    modifier: Modifier = Modifier,
) {
    state.displayNotification?.let {
        if (it.syncNotificationType == SyncNotificationType.NOT_CONNECTED_TO_WIFI) {
            InlineWarningBanner(
                modifier = modifier,
                title = stringResource(it.title),
                message = stringResource(it.text),
                titleStyle = MaterialTheme.typography.body3,
                messageStyle = MaterialTheme.typography.body3,
                onCloseClick = {
                    onDismissNotification()
                },
            )
        }
    }
}
