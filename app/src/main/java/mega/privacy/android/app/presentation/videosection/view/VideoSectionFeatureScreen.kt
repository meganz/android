package mega.privacy.android.app.presentation.videosection.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import mega.privacy.android.app.presentation.videosection.VideoSectionViewModel
import mega.privacy.android.app.presentation.videosection.model.VideoPlaylistUIEntity
import mega.privacy.android.app.presentation.videosection.model.VideoUIEntity
import mega.privacy.android.app.presentation.videosection.view.playlist.VideoPlaylistDetailView
import mega.privacy.android.app.presentation.videosection.view.playlist.videoPlaylistDetailRoute

@Composable
internal fun VideoSectionFeatureScreen(
    videoSectionViewModel: VideoSectionViewModel,
    onClick: (item: VideoUIEntity, index: Int) -> Unit,
    onAddElementsClicked: () -> Unit,
    onSortOrderClick: () -> Unit,
    onMenuClick: (VideoUIEntity) -> Unit,
    onLongClick: (item: VideoUIEntity, index: Int) -> Unit,
    onPlaylistDetailItemClick: (item: VideoUIEntity, index: Int) -> Unit,
    onPlaylistDetailItemLongClick: (item: VideoUIEntity, index: Int) -> Unit,
    onPlaylistItemClick: (item: VideoPlaylistUIEntity, index: Int) -> Unit,
    onPlaylistItemLongClick: (VideoPlaylistUIEntity, index: Int) -> Unit,
    onPlaylistItemMenuClick: (VideoPlaylistUIEntity) -> Unit = {},
) {
    val navHostController = rememberNavController()
    val route = navHostController.currentDestination?.route

    LaunchedEffect(route) {
        route?.let { videoSectionViewModel.setCurrentDestinationRoute(it) }
    }

    VideoSectionNavHost(
        modifier = Modifier,
        navHostController = navHostController,
        viewModel = videoSectionViewModel,
        onClick = onClick,
        onSortOrderClick = onSortOrderClick,
        onMenuClick = onMenuClick,
        onLongClick = onLongClick,
        onPlaylistDetailItemClick = onPlaylistDetailItemClick,
        onPlaylistItemClick = onPlaylistItemClick,
        onPlaylistItemLongClick = onPlaylistItemLongClick,
        onAddElementsClicked = onAddElementsClicked,
        onPlaylistDetailLongClicked = onPlaylistDetailItemLongClick,
    )
}

@Composable
internal fun VideoSectionNavHost(
    navHostController: NavHostController,
    onClick: (item: VideoUIEntity, index: Int) -> Unit,
    onSortOrderClick: () -> Unit,
    onMenuClick: (VideoUIEntity) -> Unit,
    onLongClick: (item: VideoUIEntity, index: Int) -> Unit,
    onPlaylistItemClick: (VideoPlaylistUIEntity, index: Int) -> Unit,
    onPlaylistItemLongClick: (VideoPlaylistUIEntity, index: Int) -> Unit,
    onPlaylistDetailItemClick: (item: VideoUIEntity, index: Int) -> Unit,
    onAddElementsClicked: () -> Unit,
    onPlaylistDetailLongClicked: (item: VideoUIEntity, index: Int) -> Unit,
    modifier: Modifier,
    onPlaylistItemMenuClick: (VideoPlaylistUIEntity) -> Unit = {},
    viewModel: VideoSectionViewModel = hiltViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    if (state.isVideoPlaylistCreatedSuccessfully) {
        viewModel.setIsVideoPlaylistCreatedSuccessfully(false)
        navHostController.navigate(
            route = videoPlaylistDetailRoute,
        )
    }

    if (state.areVideoPlaylistsRemovedSuccessfully &&
        navHostController.currentDestination?.route == videoPlaylistDetailRoute
    ) {
        viewModel.setAreVideoPlaylistsRemovedSuccessfully(false)
        navHostController.popBackStack()
    }

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
                onPlaylistItemClick = { playlist, index ->
                    if (state.isInSelection) {
                        onPlaylistItemClick(playlist, index)
                    } else {
                        viewModel.updateCurrentVideoPlaylist(playlist)
                        navHostController.navigate(
                            route = videoPlaylistDetailRoute,
                        )
                    }
                },
                onPlaylistItemMenuClick = onPlaylistItemMenuClick,
                onPlaylistItemLongClick = onPlaylistItemLongClick
            )
        }
        composable(
            route = videoPlaylistDetailRoute
        ) {
            VideoPlaylistDetailView(
                playlist = state.currentVideoPlaylist,
                isInputTitleValid = state.isInputTitleValid,
                shouldDeleteVideoPlaylistDialog = state.shouldDeleteSingleVideoPlaylist,
                shouldRenameVideoPlaylistDialog = state.shouldRenameVideoPlaylist,
                shouldShowVideoPlaylistBottomSheetDetails = state.shouldShowMoreVideoPlaylistOptions,
                numberOfAddedVideos = state.numberOfAddedVideos,
                addedMessageShown = viewModel::clearNumberOfAddedVideos,
                setShouldDeleteVideoPlaylistDialog = viewModel::setShouldDeleteSingleVideoPlaylist,
                setShouldRenameVideoPlaylistDialog = viewModel::setShouldRenameVideoPlaylist,
                setShouldShowVideoPlaylistBottomSheetDetails = viewModel::setShouldShowMoreVideoPlaylistOptions,
                inputPlaceHolderText = state.createVideoPlaylistPlaceholderTitle,
                setInputValidity = viewModel::setNewPlaylistTitleValidity,
                onRenameDialogPositiveButtonClicked = viewModel::updateVideoPlaylistTitle,
                onDeleteDialogPositiveButtonClicked = { playlist ->
                    viewModel.removeVideoPlaylists(listOf(playlist))
                },
                onAddElementsClicked = onAddElementsClicked,
                errorMessage = state.createDialogErrorMessage,
                onClick = onPlaylistDetailItemClick,
                onMenuClick = onMenuClick,
                onLongClick = onPlaylistDetailLongClicked
            )
        }
    }
}