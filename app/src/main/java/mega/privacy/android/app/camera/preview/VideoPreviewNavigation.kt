package mega.privacy.android.app.camera.preview

import android.net.Uri
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

/**
 * Video preview screen
 */
fun NavGraphBuilder.videoPreviewScreen(
    onBackPressed: () -> Unit,
    onSendVideo: (Uri) -> Unit,
) {
    composable(
        route = "$VIDEO_PREVIEW_ROUTE?video_uri={$VIDEO_URI_ARG}",
        arguments = listOf(
            navArgument(name = VIDEO_URI_ARG) {
                type = NavType.StringType
            },
        )
    ) {
        val uri = Uri.parse(it.arguments?.getString(VIDEO_URI_ARG))
        VideoPreviewScreen(
            uri = uri,
            onBackPressed = onBackPressed,
            onSendVideo = onSendVideo
        )
    }
}

/**
 * Navigate to video preview
 *
 * @param uri
 * @param navOptions
 */
fun NavController.navigateToVideoPreview(
    uri: Uri,
    navOptions: NavOptions? = null,
) {
    navigate(
        route = "$VIDEO_PREVIEW_ROUTE?$VIDEO_URI_ARG=$uri",
        navOptions
    )
}

/**
 * Video preview route
 */
const val VIDEO_PREVIEW_ROUTE = "camera/preview/video"

/**
 * Video uri arg
 */
const val VIDEO_URI_ARG = "video_uri"