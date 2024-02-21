package mega.privacy.android.app.settings.camerauploads.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import mega.privacy.android.app.settings.camerauploads.navigation.routes.SettingsCameraUploadsRoute

/**
 * Route for [SettingsCameraUploadsRoute]
 */
internal const val SETTINGS_CAMERA_UPLOADS_ROUTE = "settingsCameraUploads/main"

/**
 * Builds the Navigation Graph of Settings Camera Uploads
 */
internal fun NavGraphBuilder.settingsCameraUploadsScreen() {
    composable(SETTINGS_CAMERA_UPLOADS_ROUTE) {
        SettingsCameraUploadsRoute()
    }
}