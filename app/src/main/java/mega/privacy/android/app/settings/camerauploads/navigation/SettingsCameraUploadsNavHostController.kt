package mega.privacy.android.app.settings.camerauploads.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost

/**
 * A Composable that builds the [NavHostController] for Settings Camera Uploads
 *
 * @param modifier The [Modifier]
 * @param navHostController The [NavHostController] of the feature
 */
@Composable
internal fun SettingsCameraUploadsNavHostController(
    modifier: Modifier,
    navHostController: NavHostController,
) {
    NavHost(
        modifier = modifier,
        navController = navHostController,
        startDestination = SETTINGS_CAMERA_UPLOADS_ROUTE,
    ) {
        settingsCameraUploadsScreen()
    }
}