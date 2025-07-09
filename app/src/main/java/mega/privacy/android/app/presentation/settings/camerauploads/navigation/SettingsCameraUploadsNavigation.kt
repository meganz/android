package mega.privacy.android.app.presentation.settings.camerauploads.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.settings.camerauploads.navigation.routes.SettingsCameraUploadsRoute

@Serializable
data object SettingsCameraUploads

/**
 * Builds the Navigation Graph of Settings Camera Uploads
 *
 * @param isShowHowToUploadPrompt Boolean indicating whether to show the how-to upload prompt
 */
internal fun NavGraphBuilder.settingsCameraUploadsScreen(isShowHowToUploadPrompt: Boolean) {
    composable<SettingsCameraUploads> {
        SettingsCameraUploadsRoute(isShowHowToUploadPrompt)
    }
}