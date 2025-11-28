package mega.privacy.android.core.sharedcomponents.handler

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

/**
 * Data class to hold app settings handler function.
 */
data class AppSettingsHandler(
    val onPermissionDenied: () -> Unit,
)

/**
 * Composable function that provides app settings functionality for permission handling.
 *
 * This composable encapsulates the logic for handling permission denial by showing
 * a snackbar with an action to open app settings, and checking permissions after
 * returning from settings.
 *
 * @param message The message to display in the snackbar when permission is denied
 * @param actionLabel The label for the snackbar action button
 * @param permissions The permissions to check after returning from app settings
 * @param onPermissionsGranted Callback invoked when permissions are granted after returning from settings
 * @return [AppSettingsHandler] containing the handler function to trigger when permission is denied
 */
@Composable
fun rememberAppSettingsHandler(
    message: String,
    actionLabel: String,
    vararg permissions: String,
    onPermissionsGranted: () -> Unit,
): AppSettingsHandler {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = LocalSnackBarHostState.current

    val permissionsArray = permissions.toList()

    val appSettingLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val allPermissionsGranted = permissionsArray.all { permission ->
            ActivityCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
        if (allPermissionsGranted) {
            onPermissionsGranted()
        }
    }

    return remember(appSettingLauncher, permissionsArray) {
        AppSettingsHandler(onPermissionDenied = {
            coroutineScope.launch {
                val result = snackbarHostState?.showAutoDurationSnackbar(
                    message = message,
                    actionLabel = actionLabel
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
        })
    }
}

