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
import mega.privacy.android.app.presentation.bottomsheet.NodeOptionsBottomSheetDialogFragment.Companion.CLOUD_DRIVE_MODE
import mega.privacy.android.app.presentation.bottomsheet.NodeOptionsBottomSheetDialogFragment.Companion.VIDEO_RECENTLY_WATCHED_MODE
import mega.privacy.android.app.presentation.videosection.VideoSectionViewModel
import mega.privacy.android.app.presentation.videosection.model.VideoPlaylistUIEntity
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction
import mega.privacy.android.app.presentation.videosection.model.VideoUIEntity
import mega.privacy.android.app.presentation.videosection.view.playlist.VideoPlaylistDetailView
import mega.privacy.android.app.presentation.videosection.view.playlist.videoPlaylistDetailRoute
import mega.privacy.android.app.presentation.videosection.view.recentlywatched.VideoRecentlyWatchedView
import mega.privacy.android.app.presentation.videosection.view.recentlywatched.videoRecentlyWatchedRoute

@Composable
internal fun VideoSectionFeatureScreen(
    modifier: Modifier,
    videoSectionViewModel: VideoSectionViewModel,
    onAddElementsClicked: () -> Unit,
    onSortOrderClick: () -> Unit,
    onMenuClick: (VideoUIEntity, index: Int) -> Unit,
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
    onMenuClick: (VideoUIEntity, index: Int) -> Unit,
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

    val onBackPressedDispatcher =
        LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

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
                onMenuClick = { onMenuClick(it, CLOUD_DRIVE_MODE) },
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
                onMenuAction = { action ->
                    if (action is VideoSectionMenuAction.VideoRecentlyWatchedAction) {
                        viewModel.loadRecentlyWatchedVideos()
                        navHostController.navigate(route = videoRecentlyWatchedRoute)
                    } else {
                        onMenuAction(action)
                    }
                }
            )
        }
        composable(
            route = videoPlaylistDetailRoute
        ) {

            VideoPlaylistDetailView(
                playlist = state.currentVideoPlaylist,
                selectedSize = state.selectedVideoElementIDs.size,
                accountType = state.accountDetail?.levelDetail?.accountType,
                isHideMenuActionVisible = state.isHideMenuActionVisible,
                isUnhideMenuActionVisible = state.isUnhideMenuActionVisible,
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
                onMenuClick = { onMenuClick(it, CLOUD_DRIVE_MODE) },
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

                        else -> {
                            onMenuAction(action)
                        }
                    }
                }
            )
        }

        composable(route = videoRecentlyWatchedRoute) {
            VideoRecentlyWatchedView(
                group = state.groupedVideoRecentlyWatchedItems,
                accountType = state.accountDetail?.levelDetail?.accountType,
                clearRecentlyWatchedVideosSuccess = state.clearRecentlyWatchedVideosSuccess,
                removeRecentlyWatchedItemSuccess = state.removeRecentlyWatchedItemSuccess,
                modifier = Modifier,
                onBackPressed = { onBackPressedDispatcher?.onBackPressed() },
                onClick = viewModel::onItemClicked,
                onActionPressed = {
                    if (it is VideoSectionMenuAction.VideoRecentlyWatchedClearAction) {
                        viewModel.clearRecentlyWatchedVideos()
                    }
                },
                onMenuClick = { onMenuClick(it, VIDEO_RECENTLY_WATCHED_MODE) },
                clearRecentlyWatchedVideosMessageShown = viewModel::resetClearRecentlyWatchedVideosSuccess,
                removedRecentlyWatchedItemMessageShown = viewModel::resetRemoveRecentlyWatchedItemSuccess
            )
        }
    }
}