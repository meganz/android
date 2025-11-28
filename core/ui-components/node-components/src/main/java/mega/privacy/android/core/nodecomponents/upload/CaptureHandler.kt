package mega.privacy.android.core.nodecomponents.upload

import android.Manifest.permission.CAMERA
import android.Manifest.permission.RECORD_AUDIO
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import mega.privacy.android.core.nodecomponents.R
import mega.privacy.android.core.sharedcomponents.handler.rememberAppSettingsHandler
import mega.privacy.android.navigation.MegaActivityResultContract
import mega.privacy.android.navigation.camera.CameraArg
import mega.privacy.android.navigation.extensions.rememberMegaResultContract

/**
 * Data class to hold capture handler function.
 */
data class CaptureHandler(
    val onCaptureClicked: () -> Unit,
)

/**
 * Composable function that provides capture (camera) functionality.
 *
 * This composable encapsulates all the launcher logic for capturing photos/videos,
 * including permission handling and camera activity integration.
 *
 * @param onPhotoCaptured Callback invoked when a photo is captured, receives the URI
 * @param megaResultContract Optional contract provider, defaults to rememberMegaResultContract()
 * @return [CaptureHandler] containing the handler function to trigger capture
 */
@Composable
fun rememberCaptureHandler(
    onPhotoCaptured: (Uri) -> Unit,
    megaResultContract: MegaActivityResultContract = rememberMegaResultContract(),
): CaptureHandler {
    val context = LocalContext.current

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = megaResultContract.inAppCameraResultContract
    ) { uri: Uri? ->
        if (uri != null) {
            onPhotoCaptured(uri)
        }
    }

    val appSettingsHandler = rememberAppSettingsHandler(
        message = context.getString(R.string.chat_attach_pick_from_camera_deny_permission),
        actionLabel = context.getString(R.string.general_allow),
        CAMERA,
        onPermissionsGranted = {
            takePictureLauncher.launch(
                CameraArg(
                    title = context.getString(R.string.context_upload),
                    buttonText = context.getString(R.string.context_upload)
                )
            )
        }
    )

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsResult ->
        if (permissionsResult[CAMERA] == true) {
            takePictureLauncher.launch(
                CameraArg(
                    title = context.getString(R.string.context_upload),
                    buttonText = context.getString(R.string.context_upload)
                )
            )
        } else {
            appSettingsHandler.onPermissionDenied()
        }
    }

    return remember(cameraPermissionLauncher) {
        CaptureHandler(onCaptureClicked = {
            cameraPermissionLauncher.launch(arrayOf(CAMERA, RECORD_AUDIO))
        })
    }
}

