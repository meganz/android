package mega.privacy.android.app.presentation.videosection.view

import androidx.compose.runtime.Composable
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
    modifier: Modifier,
    videoSectionViewModel: VideoSectionViewModel,
    onAddElementsClicked: () -> Unit,
    onSortOrderClick: () -> Unit,
    onMenuClick: (VideoUIEntity) -> Unit,
    onLongClick: (item: VideoUIEntity, index: Int) -> Unit,
    onPlaylistDetailItemLongClick: (item: VideoUIEntity, index: Int) -> Unit,
    onPlaylistItemLongClick: (VideoPlaylistUIEntity, index: Int) -> Unit,
    onActionModeFinished: () -> Unit,
    onPlaylistItemMenuClick: (VideoPlaylistUIEntity) -> Unit = {},
) {
    val navHostController = rememberNavController()

    VideoSectionNavHost(
        modifier = modifier,
        navHostController = navHostController,
        viewModel = videoSectionViewModel,
        onSortOrderClick = onSortOrderClick,
        onMenuClick = onMenuClick,
        onLongClick = onLongClick,
        onPlaylistItemLongClick = onPlaylistItemLongClick,
        onAddElementsClicked = onAddElementsClicked,
        onPlaylistDetailLongClicked = onPlaylistDetailItemLongClick,
        onActionModeFinished = onActionModeFinished
    )
}

@Composable
internal fun VideoSectionNavHost(
    navHostController: NavHostController,
    onSortOrderClick: () -> Unit,
    onMenuClick: (VideoUIEntity) -> Unit,
    onLongClick: (item: VideoUIEntity, index: Int) -> Unit,
    onPlaylistItemLongClick: (VideoPlaylistUIEntity, index: Int) -> Unit,
    onAddElementsClicked: () -> Unit,
    onPlaylistDetailLongClicked: (item: VideoUIEntity, index: Int) -> Unit,
    onActionModeFinished: () -> Unit,
    modifier: Modifier,
    onPlaylistItemMenuClick: (VideoPlaylistUIEntity) -> Unit = {},
    viewModel: VideoSectionViewModel = hiltViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value

    val onDeleteVideosDialogPositiveButtonClicked: (VideoPlaylistUIEntity) -> Unit = { playlist ->
        viewModel.setShouldDeleteVideosFromPlaylist(false)
        val removedVideoIDs = state.selectedVideoElementIDs
        viewModel.removeVideosFromPlaylist(playlist.id, removedVideoIDs)
        onActionModeFinished()
    }

    if (state.isVideoPlaylistCreatedSuccessfully) {
        viewModel.setIsVideoPlaylistCreatedSuccessfully(false)
        navHostController.navigate(
            route = videoPlaylistDetailRoute,
        )
    }

    if (state.areVideoPlaylistsRemovedSuccessfully) {
        viewModel.setAreVideoPlaylistsRemovedSuccessfully(false)
        if (navHostController.currentDestination?.route == videoPlaylistDetailRoute) {
            navHostController.popBackStack()
        }
    }

    navHostController.addOnDestinationChangedListener { _, destination, _ ->
        destination.route?.let { route ->
            viewModel.setCurrentDestinationRoute(route)
            if (route != videoPlaylistDetailRoute) {
                viewModel.updateCurrentVideoPlaylist(null)
            }
        }
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
                onClick = viewModel::onItemClicked,
                onSortOrderClick = onSortOrderClick,
                onMenuClick = onMenuClick,
                onLongClick = onLongClick,
                onPlaylistItemClick = { playlist, index ->
                    if (state.isInSelection) {
                        viewModel.onVideoPlaylistItemClicked(playlist, index)
                    } else {
                        viewModel.updateCurrentVideoPlaylist(playlist)
                        navHostController.navigate(
                            route = videoPlaylistDetailRoute,
                        )
                    }
                },
                onPlaylistItemMenuClick = onPlaylistItemMenuClick,
                onPlaylistItemLongClick = onPlaylistItemLongClick,
                onDeleteDialogButtonClicked = onActionModeFinished
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
                numberOfRemovedItems = state.numberOfRemovedItems,
                removedMessageShown = viewModel::clearNumberOfRemovedItems,
                setShouldDeleteVideoPlaylistDialog = viewModel::setShouldDeleteSingleVideoPlaylist,
                setShouldRenameVideoPlaylistDialog = viewModel::setShouldRenameVideoPlaylist,
                setShouldShowVideoPlaylistBottomSheetDetails = viewModel::setShouldShowMoreVideoPlaylistOptions,
                inputPlaceHolderText = state.createVideoPlaylistPlaceholderTitle,
                setInputValidity = viewModel::setNewPlaylistTitleValidity,
                onRenameDialogPositiveButtonClicked = viewModel::updateVideoPlaylistTitle,
                onDeleteDialogPositiveButtonClicked = viewModel::removeVideoPlaylists,
                onAddElementsClicked = onAddElementsClicked,
                errorMessage = state.createDialogErrorMessage,
                onClick = { item, index ->
                    if (navHostController.currentDestination?.route == videoPlaylistDetailRoute) {
                        viewModel.onVideoItemOfPlaylistClicked(item, index)
                    }
                },
                onMenuClick = onMenuClick,
                onLongClick = onPlaylistDetailLongClicked,
                shouldDeleteVideosDialog = state.shouldDeleteVideosFromPlaylist,
                setShouldDeleteVideosDialog = viewModel::setShouldDeleteVideosFromPlaylist,
                onDeleteVideosDialogPositiveButtonClicked = onDeleteVideosDialogPositiveButtonClicked,
                onPlayAllClicked = viewModel::playAllButtonClicked,
                onUpdatedTitle = viewModel::setUpdateToolbarTitle
            )
        }
    }
}