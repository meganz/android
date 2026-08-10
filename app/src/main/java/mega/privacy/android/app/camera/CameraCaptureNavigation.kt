package mega.privacy.android.app.camera

import android.net.Uri
import androidx.compose.material.ScaffoldState
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.compose.material.navigation.BottomSheetNavigator

/**
 * Camera capture screen
 *
 * @param onFinish Callback to be called when the screen finishes
 */
fun NavGraphBuilder.cameraCaptureScreen(
    scaffoldState: ScaffoldState,
    bottomSheetNavigator: BottomSheetNavigator,
    onFinish: (Uri?) -> Unit,
    onOpenVideoPreview: (Uri) -> Unit,
    onOpenPhotoPreview: (Uri) -> Unit,
    onOpenCameraSetting: () -> Unit,
) {
    composable(CAMERA_CAPTURE_ROUTE) {
        CameraCaptureScreen(
            scaffoldState = scaffoldState,
            bottomSheetNavigator = bottomSheetNavigator,
            onFinish = onFinish,
            onOpenVideoPreview = onOpenVideoPreview,
            onOpenPhotoPreview = onOpenPhotoPreview,
            onOpenCameraSetting = onOpenCameraSetting
        )
    }
}

/**
 * Route for [CameraCaptureScreen]
 */
const val CAMERA_CAPTURE_ROUTE = "camera/capture"