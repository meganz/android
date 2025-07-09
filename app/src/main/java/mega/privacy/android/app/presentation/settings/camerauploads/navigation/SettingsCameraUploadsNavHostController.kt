package mega.privacy.android.app.presentation.settings.camerauploads.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost

/**
 * A Composable that builds the [NavHostController] for Settings Camera Uploads
 *
 * @param modifier The [Modifier]
 * @param navHostController The [NavHostController] of the feature
 * @param isShowHowToUploadPrompt Boolean indicating whether to show the how-to upload prompt
 */
@Composable
internal fun SettingsCameraUploadsNavHostController(
    modifier: Modifier,
    navHostController: NavHostController,
    isShowHowToUploadPrompt: Boolean,
) {
    NavHost(
        modifier = modifier,
        navController = navHostController,
        startDestination = SettingsCameraUploads,
    ) {
        settingsCameraUploadsScreen(isShowHowToUploadPrompt)
    }
}