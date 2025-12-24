package mega.privacy.android.app.presentation.videosection.view

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.navigation.material.BottomSheetNavigator
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.presentation.bottomsheet.NodeOptionsBottomSheetDialogFragment.Companion.VIDEO_PLAYLIST_DETAIL
import mega.privacy.android.app.presentation.bottomsheet.NodeOptionsBottomSheetDialogFragment.Companion.VIDEO_RECENTLY_WATCHED_MODE
import mega.privacy.android.app.presentation.bottomsheet.NodeOptionsBottomSheetDialogFragment.Companion.VIDEO_SECTION_MODE
import mega.privacy.android.app.presentation.extensions.getStorageState
import mega.privacy.android.app.presentation.node.NodeActionHandler
import mega.privacy.android.app.presentation.node.NodeActionsViewModel
import mega.privacy.android.app.presentation.node.model.menuaction.DownloadMenuAction
import mega.privacy.android.app.presentation.node.model.menuaction.HideDropdownMenuAction
import mega.privacy.android.app.presentation.node.model.menuaction.SendToChatMenuAction
import mega.privacy.android.app.presentation.node.model.menuaction.UnhideDropdownMenuAction
import mega.privacy.android.app.presentation.search.model.navigation.removeNodeLinkDialogNavigation
import mega.privacy.android.app.presentation.search.navigation.cannotVerifyUserNavigation
import mega.privacy.android.app.presentation.search.navigation.changeLabelBottomSheetNavigation
import mega.privacy.android.app.presentation.search.navigation.changeNodeExtensionDialogNavigation
import mega.privacy.android.app.presentation.search.navigation.foreignNodeDialogNavigation
import mega.privacy.android.app.presentation.search.navigation.leaveFolderShareDialogNavigation
import mega.privacy.android.app.presentation.search.navigation.moveToRubbishOrDeleteNavigation
import mega.privacy.android.app.presentation.search.navigation.nodeBottomSheetNavigation
import mega.privacy.android.app.presentation.search.navigation.nodeBottomSheetRoute
import mega.privacy.android.app.presentation.search.navigation.overQuotaDialogNavigation
import mega.privacy.android.app.presentation.search.navigation.removeShareFolderDialogNavigation
import mega.privacy.android.app.presentation.search.navigation.renameDialogNavigation
import mega.privacy.android.app.presentation.search.navigation.shareFolderAccessDialogNavigation
import mega.privacy.android.app.presentation.search.navigation.shareFolderDialogNavigation
import mega.privacy.android.app.presentation.videosection.VideoSectionViewModel
import mega.privacy.android.app.presentation.videosection.model.VideoPlaylistUIEntity
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction
import mega.privacy.android.app.presentation.videosection.model.VideoUIEntity
import mega.privacy.android.app.presentation.videosection.view.playlist.VideoPlaylistDetailView
import mega.privacy.android.app.presentation.videosection.view.playlist.videoPlaylistDetailRoute
import mega.privacy.android.app.presentation.videosection.view.recentlywatched.VideoRecentlyWatchedView
import mega.privacy.android.app.presentation.videosection.view.recentlywatched.videoRecentlyWatchedRoute
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.feature.sync.data.mapper.ListToStringWithDelimitersMapper
import mega.privacy.android.shared.original.core.ui.controls.sheets.MegaBottomSheetLayout
import mega.privacy.mobile.analytics.event.RecentlyWatchedOpenedButtonPressedEvent

@Composable
internal fun VideoSectionFeatureScreen(
    modifier: Modifier,
    videoSectionViewModel: VideoSectionViewModel,
    onSortOrderClick: () -> Unit,
    onMenuClick: (VideoUIEntity, index: Int) -> Unit,
    onMenuAction: (VideoSectionMenuAction?) -> Unit,
    retryActionCallback: () -> Unit,
) {
    val navHostController = rememberNavController()

    VideoSectionNavHost(
        modifier = modifier,
        navHostController = navHostController,
        viewModel = videoSectionViewModel,
        onSortOrderClick = onSortOrderClick,
        onMenuClick = onMenuClick,
        onMenuAction = onMenuAction,
        retryActionCallback = retryActionCallback
    )
}

@Composable
internal fun VideoSectionNavHost(
    navHostController: NavHostController,
    onSortOrderClick: () -> Unit,
    onMenuClick: (VideoUIEntity, index: Int) -> Unit,
    modifier: Modifier,
    onMenuAction: (VideoSectionMenuAction?) -> Unit,
    retryActionCallback: () -> Unit,
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
                onMenuClick = { onMenuClick(it, VIDEO_SECTION_MODE) },
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
                        Analytics.tracker.trackEvent(RecentlyWatchedOpenedButtonPressedEvent)
                        navHostController.navigate(route = videoRecentlyWatchedRoute)
                    } else {
                        onMenuAction(action)
                    }
                },
                retryActionCallback = retryActionCallback
            )
        }
        composable(
            route = videoPlaylistDetailRoute
        ) {
            VideoPlaylistDetailView(
                playlist = state.currentVideoPlaylist,
                selectedSize = state.selectedVideoElementIDs.size,
                shouldApplySensitiveMode = state.hiddenNodeEnabled
                        && state.accountType?.isPaid == true
                        && !state.isBusinessAccountExpired,
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
                errorMessage = state.createDialogErrorMessage,
                onClick = { item, index ->
                    if (navHostController.currentDestination?.route == videoPlaylistDetailRoute) {
                        viewModel.onVideoItemOfPlaylistClicked(item, index)
                    }
                },
                onMenuClick = { onMenuClick(it, VIDEO_PLAYLIST_DETAIL) },
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
                        VideoSectionMenuAction.VideoSectionSelectAllAction ->
                            viewModel.selectAllVideosOfPlaylist()

                        VideoSectionMenuAction.VideoSectionClearSelectionAction ->
                            viewModel.clearAllSelectedVideosOfPlaylist()

                        else -> {
                            onMenuAction(action)
                        }
                    }
                },
                onRemoveFavouriteOptionClicked = {
                    viewModel.removeFavourites()
                    viewModel.clearAllSelectedVideosOfPlaylist()
                },
                isStorageOverQuota = { getStorageState() == StorageState.PayWall },
            )
        }

        composable(route = videoRecentlyWatchedRoute) {
            VideoRecentlyWatchedView(
                group = state.groupedVideoRecentlyWatchedItems,
                shouldApplySensitiveMode = state.hiddenNodeEnabled
                        && state.accountType?.isPaid == true
                        && !state.isBusinessAccountExpired,
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

@OptIn(ExperimentalMaterialNavigationApi::class)
@Composable
internal fun VideoSectionScreen(
    modifier: Modifier,
    videoSectionViewModel: VideoSectionViewModel,
    onSortOrderClick: () -> Unit,
    onMenuAction: (VideoSectionMenuAction?) -> Unit,
    retryActionCallback: () -> Unit,
    nodeActionHandler: NodeActionHandler,
    scaffoldState: ScaffoldState,
    fileTypeIconMapper: FileTypeIconMapper,
    listToStringWithDelimitersMapper: ListToStringWithDelimitersMapper,
    navHostController: NavHostController,
    bottomSheetNavigator: BottomSheetNavigator,
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    VideoSectionNavHost(
        modifier = modifier,
        viewModel = videoSectionViewModel,
        onSortOrderClick = onSortOrderClick,
        onMenuClick = { item, type ->
            keyboardController?.hide()
            navHostController.navigate(
                route = nodeBottomSheetRoute.plus("/${item.id.longValue}")
                    .plus("/${type.name}")
            ) {
                popUpTo(nodeBottomSheetRoute) { inclusive = true }
                launchSingleTop = true
            }
        },
        onMenuAction = onMenuAction,
        retryActionCallback = retryActionCallback,
        nodeActionHandler = nodeActionHandler,
        scaffoldState = scaffoldState,
        fileTypeIconMapper = fileTypeIconMapper,
        listToStringWithDelimitersMapper = listToStringWithDelimitersMapper,
        navHostController = navHostController,
        bottomSheetNavigator = bottomSheetNavigator,
    )
}


@OptIn(ExperimentalMaterialNavigationApi::class)
@Composable
internal fun VideoSectionNavHost(
    onSortOrderClick: () -> Unit,
    onMenuClick: (VideoUIEntity, NodeSourceType) -> Unit,
    modifier: Modifier,
    onMenuAction: (VideoSectionMenuAction?) -> Unit,
    retryActionCallback: () -> Unit,
    nodeActionHandler: NodeActionHandler,
    scaffoldState: ScaffoldState,
    fileTypeIconMapper: FileTypeIconMapper,
    listToStringWithDelimitersMapper: ListToStringWithDelimitersMapper,
    navHostController: NavHostController,
    bottomSheetNavigator: BottomSheetNavigator,
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
            if (route == videoPlaylistDetailRoute
                || route == videoRecentlyWatchedRoute
                || route == videoSectionRoute
            ) {
                viewModel.setCurrentDestinationRoute(route)
                if (route != videoPlaylistDetailRoute) {
                    viewModel.updateCurrentVideoPlaylist(null)
                }
            }
        }
    }

    val onBackPressedDispatcher =
        LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    MegaBottomSheetLayout(
        modifier = modifier,
        bottomSheetNavigator = bottomSheetNavigator,
    ) {
        NavHost(
            modifier = Modifier,
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
                    onMenuClick = { item ->
                        onMenuClick(item, NodeSourceType.VIDEOS)
                    },
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
                            Analytics.tracker.trackEvent(RecentlyWatchedOpenedButtonPressedEvent)
                            navHostController.navigate(route = videoRecentlyWatchedRoute)
                        } else {
                            onMenuAction(action)
                        }
                    },
                    retryActionCallback = retryActionCallback,
                    scaffoldState = scaffoldState,
                    navHostController = navHostController,
                    handler = nodeActionHandler,
                )
            }
            composable(
                route = videoPlaylistDetailRoute
            ) {
                val coroutineScope = rememberCoroutineScope()
                VideoPlaylistDetailView(
                    playlist = state.currentVideoPlaylist,
                    selectedSize = state.selectedVideoElementIDs.size,
                    shouldApplySensitiveMode = state.hiddenNodeEnabled
                            && state.accountType?.isPaid == true
                            && !state.isBusinessAccountExpired,
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
                    errorMessage = state.createDialogErrorMessage,
                    onClick = { item, index ->
                        if (navHostController.currentDestination?.route == videoPlaylistDetailRoute) {
                            viewModel.onVideoItemOfPlaylistClicked(item, index)
                        }
                    },
                    onMenuClick = { item ->
                        onMenuClick(item, NodeSourceType.CLOUD_DRIVE)
                    },
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
                            VideoSectionMenuAction.VideoSectionSelectAllAction ->
                                viewModel.selectAllVideosOfPlaylist()

                            VideoSectionMenuAction.VideoSectionClearSelectionAction ->
                                viewModel.clearAllSelectedVideosOfPlaylist()

                            VideoSectionMenuAction.VideoSectionDownloadAction,
                            VideoSectionMenuAction.VideoSectionSendToChatAction,
                            VideoSectionMenuAction.VideoSectionHideAction,
                            VideoSectionMenuAction.VideoSectionUnhideAction,
                                ->
                                coroutineScope.launch {
                                    val menuAction = when (action) {
                                        VideoSectionMenuAction.VideoSectionDownloadAction -> DownloadMenuAction()
                                        VideoSectionMenuAction.VideoSectionSendToChatAction -> SendToChatMenuAction()
                                        VideoSectionMenuAction.VideoSectionHideAction -> HideDropdownMenuAction()
                                        VideoSectionMenuAction.VideoSectionUnhideAction -> UnhideDropdownMenuAction()
                                        else -> return@launch
                                    }

                                    coroutineScope.launch {
                                        val selectedNodes = viewModel.getSelectedNodes()
                                        nodeActionHandler.handleAction(menuAction, selectedNodes)
                                        viewModel.clearAllSelectedVideosOfPlaylist()
                                    }
                                }

                            else -> {
                                onMenuAction(action)
                            }
                        }
                    },
                    onRemoveFavouriteOptionClicked = {
                        viewModel.removeFavourites()
                        viewModel.clearAllSelectedVideosOfPlaylist()
                    },
                    isStorageOverQuota = { getStorageState() == StorageState.PayWall },
                    scaffoldState = scaffoldState,
                )
            }

            composable(route = videoRecentlyWatchedRoute) {
                VideoRecentlyWatchedView(
                    group = state.groupedVideoRecentlyWatchedItems,
                    shouldApplySensitiveMode = state.hiddenNodeEnabled
                            && state.accountType?.isPaid == true
                            && !state.isBusinessAccountExpired,
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
                    onMenuClick = { item ->
                        onMenuClick(item, NodeSourceType.CLOUD_DRIVE)
                    },
                    clearRecentlyWatchedVideosMessageShown = viewModel::resetClearRecentlyWatchedVideosSuccess,
                    removedRecentlyWatchedItemMessageShown = viewModel::resetRemoveRecentlyWatchedItemSuccess,
                    scaffoldState = scaffoldState,
                )
            }

            moveToRubbishOrDeleteNavigation(
                navHostController = navHostController,
                listToStringWithDelimitersMapper = listToStringWithDelimitersMapper
            )
            renameDialogNavigation(navHostController = navHostController)
            nodeBottomSheetNavigation(
                nodeActionHandler = nodeActionHandler,
                navHostController = navHostController,
                fileTypeIconMapper = fileTypeIconMapper
            )
            changeLabelBottomSheetNavigation(navHostController)
            changeNodeExtensionDialogNavigation(navHostController)
            cannotVerifyUserNavigation(navHostController)
            removeNodeLinkDialogNavigation(
                navHostController = navHostController,
                listToStringWithDelimitersMapper = listToStringWithDelimitersMapper
            )
            shareFolderDialogNavigation(
                navHostController = navHostController,
                nodeActionHandler = nodeActionHandler,
                stringWithDelimitersMapper = listToStringWithDelimitersMapper
            )
            removeShareFolderDialogNavigation(
                navHostController = navHostController,
                stringWithDelimitersMapper = listToStringWithDelimitersMapper
            )
            leaveFolderShareDialogNavigation(
                navHostController = navHostController,
                stringWithDelimitersMapper = listToStringWithDelimitersMapper
            )
            overQuotaDialogNavigation(navHostController = navHostController)
            foreignNodeDialogNavigation(navHostController = navHostController)
            shareFolderAccessDialogNavigation(
                navHostController = navHostController,
                listToStringWithDelimitersMapper = listToStringWithDelimitersMapper
            )
        }
    }
}