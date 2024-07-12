package mega.privacy.android.app.presentation.videosection.view

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
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
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction
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
    onMenuAction: (VideoSectionMenuAction?) -> Unit,
) {
    val navHostController = rememberNavController()

    VideoSectionNavHost(
        modifier = modifier,
        navHostController = navHostController,
        viewModel = videoSectionViewModel,
        onSortOrderClick = onSortOrderClick,
        onMenuClick = onMenuClick,
        onAddElementsClicked = onAddElementsClicked,
        onMenuAction = onMenuAction,
    )
}

@Composable
internal fun VideoSectionNavHost(
    navHostController: NavHostController,
    onSortOrderClick: () -> Unit,
    onMenuClick: (VideoUIEntity) -> Unit,
    onAddElementsClicked: () -> Unit,
    modifier: Modifier,
    onMenuAction: (VideoSectionMenuAction?) -> Unit,
    viewModel: VideoSectionViewModel = hiltViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value

    val onDeleteVideosDialogPositiveButtonClicked: (VideoPlaylistUIEntity) -> Unit = { playlist ->
        val removedVideoIDs = state.selectedVideoElementIDs
        viewModel.removeVideosFromPlaylist(playlist.id, removedVideoIDs)
        viewModel.clearAllSelectedVideosOfPlaylist()
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
                onLongClick = viewModel::onItemLongClicked,
                onPlaylistItemClick = { playlist, index ->
                    if (state.isInSelection) {
                        viewModel.onVideoPlaylistItemClicked(playlist, index)
                    } else {
                        viewModel.updateCurrentVideoPlaylist(playlist)
                        navHostController.navigate(route = videoPlaylistDetailRoute)
                    }
                },
                onPlaylistItemLongClick = viewModel::onVideoPlaylistItemClicked,
                onDeleteDialogButtonClicked = viewModel::clearAllSelectedVideoPlaylists,
                onMenuAction = onMenuAction
            )
        }
        composable(
            route = videoPlaylistDetailRoute
        ) {
            val onBackPressedDispatcher =
                LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
            VideoPlaylistDetailView(
                playlist = state.currentVideoPlaylist,
                selectedSize = state.selectedVideoElementIDs.size,
                isInputTitleValid = state.isInputTitleValid,
                numberOfAddedVideos = state.numberOfAddedVideos,
                addedMessageShown = viewModel::clearNumberOfAddedVideos,
                numberOfRemovedItems = state.numberOfRemovedItems,
                removedMessageShown = viewModel::clearNumberOfRemovedItems,
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
                onLongClick = viewModel::onVideoItemOfPlaylistLongClicked,
                onDeleteVideosDialogPositiveButtonClicked = onDeleteVideosDialogPositiveButtonClicked,
                onPlayAllClicked = viewModel::playAllButtonClicked,
                onBackPressed = {
                    if (state.selectedVideoElementIDs.isNotEmpty()) {
                        viewModel.clearAllSelectedVideosOfPlaylist()
                    } else {
                        onBackPressedDispatcher?.onBackPressed()
                    }
                },
                onMenuActionClick = { action ->
                    when (action) {
                        is VideoSectionMenuAction.VideoSectionSelectAllAction ->
                            viewModel.selectAllVideosOfPlaylist()

                        is VideoSectionMenuAction.VideoSectionClearSelectionAction ->
                            viewModel.clearAllSelectedVideosOfPlaylist()

                        else -> {}
                    }
                }
            )
        }
    }
}