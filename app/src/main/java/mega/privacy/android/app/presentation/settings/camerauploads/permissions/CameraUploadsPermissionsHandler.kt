package mega.privacy.android.app.presentation.settings.camerauploads.permissions

import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import de.palm.composestateevents.EventEffect
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import mega.privacy.android.app.extensions.navigateToAppSettings
import mega.privacy.android.app.presentation.settings.camerauploads.dialogs.MediaPermissionsRationaleDialog

/**
 * A Composable that checks the Camera Uploads permissions necessary to run the feature
 *
 * @param requestPermissions State Event to request the Camera Uploads permissions
 * @param onMediaPermissionsGranted Lambda to execute when the User has granted the Media Permissions
 * @param onRequestPermissionsStateChanged Lambda to execute whether a Camera Uploads permissions
 * request should be done (triggered) or not (consumed)
 * @param modifier The [Modifier] class
 */
@Composable
internal fun CameraUploadsPermissionsHandler(
    requestPermissions: StateEvent,
    onMediaPermissionsGranted: () -> Unit,
    onRequestPermissionsStateChanged: (StateEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMediaPermissionsRationale by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    var requestPermissionsTimestamp by remember { mutableLongStateOf(0L) }
    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (it.areMediaPermissionsGranted()) {
            // Continue the Camera Uploads process
            onMediaPermissionsGranted.invoke()
        } else {
            // The result is returned immediately and the rationale is no longer shown. Navigate to app Settings
            if ((System.currentTimeMillis() - requestPermissionsTimestamp) < MAX_PERMISSION_RESULT_TIME) {
                context.navigateToAppSettings()
            } else {
                // Show the Rationale
                showMediaPermissionsRationale = true
            }
        }
    }

    EventEffect(
        event = requestPermissions,
        onConsumed = { onRequestPermissionsStateChanged.invoke(consumed) },
        action = { permissionsLauncher.launch(getCameraUploadsPermissions().toTypedArray()) }
    )

    if (showMediaPermissionsRationale) {
        MediaPermissionsRationaleDialog(
            modifier = modifier,
            onMediaAccessGranted = {
                requestPermissionsTimestamp = System.currentTimeMillis()
                onRequestPermissionsStateChanged.invoke(triggered)
                showMediaPermissionsRationale = false
            },
            onMediaAccessDenied = { showMediaPermissionsRationale = false },
        )
    }
}

/**
 * Retrieves the Camera Uploads permissions
 *
 * @return A list of Camera Uploads permissions
 */
private fun getCameraUploadsPermissions(): List<String> = buildList {
    if (SDK_INT < TIRAMISU) {
        // Only request the generic External Storage Permission on Devices below API 33
        add(READ_EXTERNAL_STORAGE)
    } else {
        // Request Granular Media and Notifications Permissions beginning on API 33
        add(READ_MEDIA_IMAGES)
        add(READ_MEDIA_VIDEO)
        add(POST_NOTIFICATIONS)
    }.apply {
        if (SDK_INT >= UPSIDE_DOWN_CAKE) {
            // Request Partial Media Permissions beginning on API 34
            add(READ_MEDIA_VISUAL_USER_SELECTED)
        }
    }
}

/**
 * Checks if the User has granted the Media Permissions necessary to enable Camera Uploads. The
 * number of Permissions being checked will depend on the Device OS
 *
 * @return true if the Media Permissions are granted
 */
private fun Map<String, Boolean>.areMediaPermissionsGranted() =
    when {
        SDK_INT < TIRAMISU -> this.getOrElse(READ_EXTERNAL_STORAGE) { false }
        // Media Permissions are still granted if at least the Partial Media Permission is granted
        SDK_INT >= UPSIDE_DOWN_CAKE -> {
            (this.getOrElse(READ_MEDIA_IMAGES) { false } && this.getOrElse(READ_MEDIA_VIDEO) { false })
                    || this.getOrElse(READ_MEDIA_VISUAL_USER_SELECTED) { false }
        }

        else -> this.getOrElse(READ_MEDIA_IMAGES) { false } && this.getOrElse(READ_MEDIA_VIDEO) { false }
    }

/**
 * The maximum time for a Permission Request result without showing the Rationale
 */
private const val MAX_PERMISSION_RESULT_TIME = 500L