package mega.privacy.android.app.presentation.permissions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.components.indicators.LargeHUD
import mega.privacy.android.app.presentation.permissions.view.CameraBackupPermissionsScreen
import mega.privacy.android.app.presentation.permissions.view.NotificationPermissionScreen

internal const val NEW_PERMISSIONS_SCREEN_NOTIFICATION_PERMISSION =
    "new_permissions_screen_notification_permission"
internal const val NEW_PERMISSIONS_SCREEN_CAMERA_BACKUP_PERMISSION =
    "new_permissions_screen_camera_backup_permission"
internal const val NEW_PERMISSIONS_SCREEN_LOADING = "new_permissions_screen_loading"

@Composable
fun NewPermissionsComposableScreen(
    uiState: PermissionsUIState,
    askNotificationPermission: () -> Unit = {},
    askCameraBackupPermission: () -> Unit = {},
    setNextPermission: () -> Unit = {},
    closePermissionScreen: () -> Unit = {},
    resetFinishEvent: () -> Unit = {},
) {
    val pagerState = rememberPagerState(
        initialPage = uiState.visiblePermission.ordinal,
        pageCount = { NewPermissionScreen.entries.size }
    )

    LaunchedEffect(uiState.visiblePermission) {
        val targetPage = uiState.visiblePermission.ordinal
        if (targetPage != pagerState.currentPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    // This event will trigger when there are no more permissions to show
    EventEffect(
        event = uiState.finishEvent,
        onConsumed = resetFinishEvent
    ) {
        closePermissionScreen()
    }

    HorizontalPager(
        modifier = Modifier.fillMaxSize(),
        state = pagerState,
        userScrollEnabled = false,
    ) { page ->
        when (page) {
            NewPermissionScreen.Notification.ordinal -> {
                NotificationPermissionScreen(
                    modifier = Modifier
                        .testTag(NEW_PERMISSIONS_SCREEN_NOTIFICATION_PERMISSION)
                        .fillMaxSize(),
                    onEnablePermission = askNotificationPermission,
                    onSkipPermission = setNextPermission
                )
            }

            NewPermissionScreen.CameraBackup.ordinal -> {
                CameraBackupPermissionsScreen(
                    modifier = Modifier
                        .testTag(NEW_PERMISSIONS_SCREEN_CAMERA_BACKUP_PERMISSION)
                        .fillMaxSize(),
                    onEnablePermission = askCameraBackupPermission,
                    onSkipPermission = setNextPermission
                )
            }

            NewPermissionScreen.Loading.ordinal -> {
                // Show loading indicator when permissions is loading
                Box(
                    modifier = Modifier
                        .testTag(NEW_PERMISSIONS_SCREEN_LOADING)
                        .fillMaxSize()
                ) {
                    LargeHUD(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}