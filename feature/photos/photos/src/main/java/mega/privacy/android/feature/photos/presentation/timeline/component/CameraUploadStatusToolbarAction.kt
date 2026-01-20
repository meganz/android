package mega.privacy.android.feature.photos.presentation.timeline.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import mega.privacy.android.feature.photos.components.CameraUploadsStatusIcon
import mega.privacy.android.feature.photos.components.CameraUploadsStatusType
import mega.privacy.android.feature.photos.presentation.CUStatusUiState
import mega.privacy.android.shared.resources.R as SharedR

@Composable
internal fun CameraUploadStatusToolbarAction(
    cameraUploadsStatus: CUStatusUiState,
    setCameraUploadsMessage: (message: String) -> Unit,
    onNavigateToCameraUploadsSettings: () -> Unit,
    onNavigateToCameraUploadsProgressScreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    when (cameraUploadsStatus) {
        is CUStatusUiState.Warning -> {
            CameraUploadsStatusIcon(
                modifier = modifier.testTag(CAMERA_UPLOAD_STATUS_TOOLBAR_ACTION_WARNING_TAG),
                type = CameraUploadsStatusType.Warning,
                enabled = false
            )
        }

        is CUStatusUiState.Disabled -> {
            CameraUploadsStatusIcon(
                modifier = modifier.testTag(CAMERA_UPLOAD_STATUS_TOOLBAR_ACTION_DEFAULT_TAG),
                type = CameraUploadsStatusType.Default,
                onClick = onNavigateToCameraUploadsSettings
            )
        }

        is CUStatusUiState.UpToDate -> {
            CameraUploadsStatusIcon(
                modifier = modifier.testTag(CAMERA_UPLOAD_STATUS_TOOLBAR_ACTION_COMPLETE_TAG),
                type = CameraUploadsStatusType.UpToDate,
                onClick = {
                    setCameraUploadsMessage(
                        context.getString(SharedR.string.media_main_screen_camera_uploads_updated),
                    )
                }
            )
        }

        is CUStatusUiState.Sync -> {
            CameraUploadsStatusIcon(
                modifier = modifier.testTag(CAMERA_UPLOAD_STATUS_TOOLBAR_ACTION_SYNC_TAG),
                type = CameraUploadsStatusType.Sync,
                enabled = false
            )
        }

        is CUStatusUiState.UploadInProgress -> {
            CameraUploadsStatusIcon(
                modifier = modifier.testTag(
                    CAMERA_UPLOAD_STATUS_TOOLBAR_ACTION_UPLOAD_IN_PROGRESS_TAG
                ),
                type = CameraUploadsStatusType.UploadInProgress,
                progress = { cameraUploadsStatus.progress },
                onClick = onNavigateToCameraUploadsProgressScreen
            )
        }

        is CUStatusUiState.UploadComplete -> {
            CameraUploadsStatusIcon(
                modifier = modifier.testTag(CAMERA_UPLOAD_STATUS_TOOLBAR_ACTION_UPLOAD_COMPLETE_TAG),
                type = CameraUploadsStatusType.UploadComplete,
                progress = { 1F },
                enabled = false
            )
        }

        else -> Unit
    }
}

internal const val CAMERA_UPLOAD_STATUS_TOOLBAR_ACTION_WARNING_TAG =
    "camera_upload_status_toolbar_action:icon_warning"
internal const val CAMERA_UPLOAD_STATUS_TOOLBAR_ACTION_DEFAULT_TAG =
    "camera_upload_status_toolbar_action:icon_default"
internal const val CAMERA_UPLOAD_STATUS_TOOLBAR_ACTION_COMPLETE_TAG =
    "camera_upload_status_toolbar_action:icon_complete"
internal const val CAMERA_UPLOAD_STATUS_TOOLBAR_ACTION_SYNC_TAG =
    "camera_upload_status_toolbar_action:icon_sync"
internal const val CAMERA_UPLOAD_STATUS_TOOLBAR_ACTION_UPLOAD_IN_PROGRESS_TAG =
    "camera_upload_status_toolbar_action:icon_upload_in_progress"
internal const val CAMERA_UPLOAD_STATUS_TOOLBAR_ACTION_UPLOAD_COMPLETE_TAG =
    "camera_upload_status_toolbar_action:icon_upload_complete"
