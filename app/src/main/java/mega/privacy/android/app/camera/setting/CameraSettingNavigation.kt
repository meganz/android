package mega.privacy.android.app.camera.setting

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.bottomSheet

internal const val CAMERA_SETTING_ROUTE = "cameraSetting"

@OptIn(ExperimentalMaterialNavigationApi::class)
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