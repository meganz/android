package mega.privacy.android.app.presentation.photos.compose.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.navigation

const val photosRoute = "photos"

private const val mainRoute = "photos/main"
private const val filterRoute = "photos/filter"
private const val albumContentRoute = "photos/album/content"
private const val albumPhotosSelectionRoute = "photos/album/photos-selection"
private const val albumCoverSelectionRoute = "photos/album/cover-selection"

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.photosNavGraph(navController: NavController) {
    navigation(startDestination = mainRoute, route = photosRoute) {
        composable(route = mainRoute) {}
        composable(route = filterRoute) {}
        composable(route = albumContentRoute) {}
        composable(route = albumPhotosSelectionRoute) {}
        composable(route = albumCoverSelectionRoute) {}
    }
}
