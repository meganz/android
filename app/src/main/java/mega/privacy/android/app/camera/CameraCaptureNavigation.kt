package mega.privacy.android.app.camera

import android.net.Uri
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/**
 * Camera capture screen
 *
 * @param onFinish Callback to be called when the screen finishes
 */
fun NavGraphBuilder.cameraCaptureScreen(
    onFinish: (Uri?) -> Unit,
    onOpenVideoPreview: (Uri) -> Unit,
    onOpenPhotoPreview: (Uri) -> Unit,
) {
    composable(CAMERA_CAPTURE_ROUTE) {
        CameraCaptureScreen(
            onFinish = onFinish,
            onOpenVideoPreview = onOpenVideoPreview,
            onOpenPhotoPreview = onOpenPhotoPreview,
        )
    }
}

/**
 * Route for [CameraCaptureScreen]
 */
const val CAMERA_CAPTURE_ROUTE = "camera/capture"