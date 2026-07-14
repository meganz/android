package mega.privacy.android.feature.photos.presentation.timeline

import android.Manifest.permission.POST_NOTIFICATIONS
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalResources
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.launch
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.privacy.android.core.sharedcomponents.permission.getCameraUploadsPermissions
import mega.privacy.android.feature.photos.presentation.MediaCameraUploadUiState
import mega.privacy.android.shared.resources.R as sharedR
import timber.log.Timber

/**
 * The callbacks [mega.privacy.android.feature.photos.presentation.timeline.component.CameraUploadsBanner]
 * needs to request the Camera Uploads media permissions and, on API 33+, the notification permission.
 */
internal class CameraUploadsBannerHandlers(
    val onChangeCameraUploadsPermissions: () -> Unit,
    val onRequestNotificationPermission: () -> Unit,
)

/**
 * Owns the permission launchers behind the Camera Uploads banner and the "uploads completed"
 * snackbar, shared by the legacy and revamped Timeline screens.
 */
@Composable
internal fun rememberCameraUploadsBannerHandlers(
    mediaCameraUploadUiState: MediaCameraUploadUiState,
    clearCameraUploadsCompletedMessage: () -> Unit,
    handleCameraUploadsPermissionsResult: () -> Unit,
    handleNotificationPermissionResult: () -> Unit,
): CameraUploadsBannerHandlers {
    val activity = LocalActivity.current
    val resources = LocalResources.current
    val snackBarHostState = LocalSnackBarHostState.current
    val scope = rememberCoroutineScope()

    EventEffect(
        event = mediaCameraUploadUiState.uploadComplete,
        onConsumed = clearCameraUploadsCompletedMessage,
    ) { count ->
        scope.launch {
            snackBarHostState?.showAutoDurationSnackbar(
                message = resources.getQuantityString(
                    sharedR.plurals.timeline_tab_camera_uploads_completed,
                    count,
                    count,
                )
            )
        }
    }

    val cameraUploadsPermissionsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            handleCameraUploadsPermissionsResult()
        }

    var isNotificationPermissionPermanentlyDenied by rememberSaveable { mutableStateOf(false) }
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                activity?.let { currentActivity ->
                    val shouldShowRationale = ActivityCompat
                        .shouldShowRequestPermissionRationale(currentActivity, POST_NOTIFICATIONS)
                    if (!shouldShowRationale) {
                        isNotificationPermissionPermanentlyDenied = true
                    }
                }
            }
            handleNotificationPermissionResult()
        }

    return CameraUploadsBannerHandlers(
        onChangeCameraUploadsPermissions = {
            cameraUploadsPermissionsLauncher.launch(getCameraUploadsPermissions())
        },
        onRequestNotificationPermission = {
            if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (isNotificationPermissionPermanentlyDenied) {
                    activity.openNotificationSettings()
                } else {
                    notificationPermissionLauncher.launch(POST_NOTIFICATIONS)
                }
            }
        },
    )
}

@SuppressLint("QueryPermissionsNeeded")
private fun Activity.openNotificationSettings() {
    val notificationIntent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    }
    val intent = if (notificationIntent.resolveActivity(packageManager) != null) {
        notificationIntent
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = "package:$packageName".toUri()
        }
    }
    runCatching {
        startActivity(intent.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
    }.onFailure { Timber.e(it, "Failed to open notification settings") }
}
