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
        onHowToUploadPromptOptionSelected = viewModel::onHowToUploadPromptOptionSelected,
        onMediaPermissionsGranted = viewModel::onMediaPermissionsGranted,
        onRegularBusinessAccountSubUserPromptAcknowledged = viewModel::onRegularBusinessAccountSubUserPromptAcknowledged,
        onRequestPermissionsStateChanged = viewModel::onRequestPermissionsStateChanged,
        onSettingsScreenPaused = viewModel::onSettingsScreenPaused,
    )
}