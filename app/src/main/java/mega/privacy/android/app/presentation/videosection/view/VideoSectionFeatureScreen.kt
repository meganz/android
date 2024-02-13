package mega.privacy.android.app.presentation.videosection.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import mega.privacy.android.app.presentation.videosection.VideoSectionViewModel
import mega.privacy.android.app.presentation.videosection.model.UIVideo
import mega.privacy.android.app.presentation.videosection.model.UIVideoPlaylist
import mega.privacy.android.app.presentation.videosection.view.playlist.VideoPlaylistDetailView
import mega.privacy.android.app.presentation.videosection.view.playlist.videoPlaylistDetailRoute

@Composable
internal fun VideoSectionFeatureScreen(
    videoSectionViewModel: VideoSectionViewModel,
    onClick: (item: UIVideo, index: Int) -> Unit,
    onSortOrderClick: () -> Unit = {},
    onMenuClick: (UIVideo) -> Unit = {},
    onLongClick: (item: UIVideo, index: Int) -> Unit = { _, _ -> },
    onPlaylistItemClick: (item: UIVideo, index: Int) -> Unit = { _, _ -> },
    onPlaylistItemMenuClick: (UIVideoPlaylist) -> Unit = { _ -> },
    onPlaylistItemLongClick: (UIVideoPlaylist, index: Int) -> Unit = { _, _ -> },
) {
    val navHostController = rememberNavController()
    VideoSectionNavHost(
        modifier = Modifier,
        navHostController = navHostController,
        viewModel = videoSectionViewModel,
        onClick = onClick,
        onSortOrderClick = onSortOrderClick,
        onMenuClick = onMenuClick,
        onLongClick = onLongClick,
        onPlaylistDetailItemClick = onPlaylistItemClick,
        onPlaylistItemMenuClick = onPlaylistItemMenuClick,
        onPlaylistItemLongClick = onPlaylistItemLongClick,
    )
}

@Composable
internal fun VideoSectionNavHost(
    navHostController: NavHostController,
    onClick: (item: UIVideo, index: Int) -> Unit,
    onSortOrderClick: () -> Unit,
    onMenuClick: (UIVideo) -> Unit,
    onLongClick: (item: UIVideo, index: Int) -> Unit,
    onPlaylistItemMenuClick: (UIVideoPlaylist) -> Unit,
    onPlaylistItemLongClick: (UIVideoPlaylist, index: Int) -> Unit,
    onPlaylistDetailItemClick: (item: UIVideo, index: Int) -> Unit,
    modifier: Modifier,
    viewModel: VideoSectionViewModel = hiltViewModel(),
) {
    NavHost(
        modifier = modifier,
        navController = navHostController,
        startDestination = videoSectionRoute
    ) {
        composable(
            route = videoSectionRoute
        ) {
            VideoSectionComposeView(
                videoSectionViewModel = viewModel,
                onClick = onClick,
                onSortOrderClick = onSortOrderClick,
                onMenuClick = onMenuClick,
                onLongClick = onLongClick,
                onPlaylistItemClick = { playlist, _ ->
                    viewModel.updateCurrentVideoPlaylist(playlist)
                    navHostController.navigate(
                        route = videoPlaylistDetailRoute,
                    )
                },
                onPlaylistItemMenuClick = onPlaylistItemMenuClick,
                onPlaylistItemLongClick = onPlaylistItemLongClick
            )
        }
        composable(
            route = videoPlaylistDetailRoute
        ) {
            VideoPlaylistDetailView(
                videoSectionViewModel = viewModel,
                onClick = onPlaylistDetailItemClick,
            )
        }
    }
}