package mega.privacy.android.app.presentation.settings.camerauploads.navigation.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.app.presentation.settings.camerauploads.SettingsCameraUploadsView
import mega.privacy.android.app.presentation.settings.camerauploads.SettingsCameraUploadsViewModel

/**
 * A Route Composable to the main Settings Camera Uploads screen
 *
 * @param viewModel The ViewModel responsible for all business logic
 */
@Composable
internal fun SettingsCameraUploadsRoute(
    viewModel: SettingsCameraUploadsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsCameraUploadsView(
        uiState = uiState,
        onBusinessAccountPromptDismissed = viewModel::onBusinessAccountPromptDismissed,
        onCameraUploadsStateChanged = viewModel::onCameraUploadsStateChanged,
        onCameraUploadsProcessStarted = viewModel::onCameraUploadsProcessStarted,
        onChargingDuringVideoCompressionStateChanged = viewModel::onChargingDuringVideoCompressionStateChanged,
        onChargingWhenUploadingContentStateChanged = viewModel::onChargingWhenUploadingContentStateChanged,
        onHowToUploadPromptOptionSelected = viewModel::onHowToUploadPromptOptionSelected,
        onIncludeLocationTagsStateChanged = viewModel::onIncludeLocationTagsStateChanged,
        onKeepFileNamesStateChanged = viewModel::onKeepFileNamesStateChanged,
        onLocalPrimaryFolderSelected = viewModel::onLocalPrimaryFolderSelected,
        onLocalSecondaryFolderSelected = viewModel::onLocalSecondaryFolderSelected,
        onLocationPermissionGranted = viewModel::onLocationPermissionGranted,
        onMediaPermissionsGranted = viewModel::onMediaPermissionsGranted,
        onMediaUploadsStateChanged = viewModel::onMediaUploadsStateChanged,
        onNewVideoCompressionSizeLimitProvided = viewModel::onNewVideoCompressionSizeLimitProvided,
        onPrimaryFolderNodeSelected = viewModel::onPrimaryFolderNodeSelected,
        onRegularBusinessAccountSubUserPromptAcknowledged = viewModel::onRegularBusinessAccountSubUserPromptAcknowledged,
        onRelatedNewLocalFolderWarningDismissed = viewModel::onRelatedNewLocalFolderWarningDismissed,
        onRequestLocationPermissionStateChanged = viewModel::onRequestLocationPermissionStateChanged,
        onRequestMediaPermissionsStateChanged = viewModel::onRequestMediaPermissionsStateChanged,
        onSecondaryFolderNodeSelected = viewModel::onSecondaryFolderNodeSelected,
        onSnackbarMessageConsumed = viewModel::onSnackbarMessageConsumed,
        onUploadOptionUiItemSelected = viewModel::onUploadOptionUiItemSelected,
        onVideoQualityUiItemSelected = viewModel::onVideoQualityUiItemSelected,
    )
}