package mega.privacy.android.app.camera.preview

import android.net.Uri
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

/**
 * Photo preview screen
 *
 * @param onBackPressed
 * @param onSendPhoto
 */
fun NavGraphBuilder.photoPreviewScreen(
    title: String,
    onBackPressed: () -> Unit,
    onSendPhoto: (Uri) -> Unit,
) {
    composable(
        route = "$PHOTO_PREVIEW_ROUTE?photo_uri={$PHOTO_URI_ARG}",
        arguments = listOf(
            navArgument(name = PHOTO_URI_ARG) {
                type = NavType.StringType
            },
        )
    ) {
        val uri = Uri.parse(it.arguments?.getString(PHOTO_URI_ARG))
        PhotoPreviewScreen(
            uri = uri,
            title = title,
            onBackPressed = onBackPressed,
            onSendPhoto = onSendPhoto
        )
    }
}

/**
 * Navigate to photo preview
 *
 * @param uri
 * @param navOptions
 */
fun NavController.navigateToPhotoPreview(
    uri: Uri,
    navOptions: NavOptions? = null,
) {
    navigate(
        route = "$PHOTO_PREVIEW_ROUTE?$PHOTO_URI_ARG=$uri",
        navOptions
    )
}

/**
 * Photo preview route
 */
const val PHOTO_PREVIEW_ROUTE = "camera/preview/photo"

/**
 * Photo uri arg
 */
const val PHOTO_URI_ARG = "photo_uri"