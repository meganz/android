package mega.privacy.android.app.camera.setting

import androidx.compose.material.navigation.bottomSheet
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController

internal const val CAMERA_SETTING_ROUTE = "cameraSetting"

internal fun NavGraphBuilder.cameraSettingModal(
    showPermissionDeniedSnackbar: () -> Unit,
) {
    bottomSheet(route = CAMERA_SETTING_ROUTE) { backStackEntry ->
        val viewModel = hiltViewModel<CameraSettingViewModel>()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        CameraSettingBottomSheet(
            state = uiState,
            onEnableGeoTagging = {
                viewModel.setSaveLocationToMedia(it)
            },
            showPermissionDeniedSnackbar = showPermissionDeniedSnackbar
        )
    }
}

internal fun NavHostController.navigateCameraSettingModal() {
    navigate(CAMERA_SETTING_ROUTE)
}