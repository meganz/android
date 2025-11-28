package mega.privacy.android.core.nodecomponents.upload

import android.Manifest.permission.CAMERA
import android.Manifest.permission.RECORD_AUDIO
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.launch
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.privacy.android.core.nodecomponents.R
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
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = LocalSnackBarHostState.current

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = megaResultContract.inAppCameraResultContract
    ) { uri: Uri? ->
        if (uri != null) {
            onPhotoCaptured(uri)
        }
    }

    val appSettingLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (ActivityCompat.checkSelfPermission(
                context,
                CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            takePictureLauncher.launch(
                CameraArg(
                    title = context.getString(R.string.context_upload),
                    buttonText = context.getString(R.string.context_upload)
                )
            )
        }
    }

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
            coroutineScope.launch {
                val result = snackbarHostState?.showAutoDurationSnackbar(
                    message = context.getString(R.string.chat_attach_pick_from_camera_deny_permission),
                    actionLabel = context.getString(R.string.general_allow)
                )
                if (result == SnackbarResult.ActionPerformed) {
                    runCatching {
                        appSettingLauncher.launch(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                        )
                    }
                }
            }
        }
    }

    return remember(cameraPermissionLauncher) {
        CaptureHandler(onCaptureClicked = {
            cameraPermissionLauncher.launch(arrayOf(CAMERA, RECORD_AUDIO))
        })
    }
}

