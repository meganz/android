package mega.privacy.android.app.presentation.videosection

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.rememberBottomSheetNavigator
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.activities.contract.NameCollisionActivityContract
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.interfaces.ActionNodeCallback
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown
import mega.privacy.android.app.modalbottomsheet.SortByBottomSheetDialogFragment
import mega.privacy.android.app.presentation.container.AppContainer
import mega.privacy.android.app.presentation.extensions.getStorageState
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.meeting.chat.extension.getInfo
import mega.privacy.android.app.presentation.node.NodeActionHandler
import mega.privacy.android.app.presentation.node.NodeActionsViewModel
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.app.presentation.psa.PsaContainer
import mega.privacy.android.app.presentation.qrcode.findActivity
import mega.privacy.android.app.presentation.search.SEARCH_SCREEN_TRANSFERS_WIDGET_TEST_TAG
import mega.privacy.android.app.presentation.search.navigation.contactArraySeparator
import mega.privacy.android.app.presentation.search.navigation.searchForeignNodeDialog
import mega.privacy.android.app.presentation.search.navigation.searchOverQuotaDialog
import mega.privacy.android.app.presentation.search.navigation.shareFolderAccessDialog
import mega.privacy.android.app.presentation.search.view.MiniAudioPlayerView
import mega.privacy.android.app.presentation.security.check.PasscodeContainer
import mega.privacy.android.app.presentation.transfers.starttransfer.view.StartTransferComponent
import mega.privacy.android.app.presentation.transfers.widget.TransfersWidget
import mega.privacy.android.app.presentation.videosection.model.DurationFilterOption
import mega.privacy.android.app.presentation.videosection.model.LocationFilterOption
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction
import mega.privacy.android.app.presentation.videosection.model.VideoSectionTab
import mega.privacy.android.app.presentation.videosection.view.VideoSectionScreen
import mega.privacy.android.app.presentation.videosection.view.videoSectionRoute
import mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.app.utils.Constants.ORDER_CLOUD
import mega.privacy.android.app.utils.Constants.ORDER_FAVOURITES
import mega.privacy.android.app.utils.Constants.ORDER_VIDEO_PLAYLIST
import mega.privacy.android.app.utils.Constants.SEARCH_BY_ADAPTER
import mega.privacy.android.app.utils.Constants.VIDEO_BROWSE_ADAPTER
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.NodeNameCollisionsResult
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.feature.sync.data.mapper.ListToStringWithDelimitersMapper
import mega.privacy.android.legacy.core.ui.model.SearchWidgetState
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar
import mega.privacy.mobile.analytics.event.AllVideosTabEvent
import mega.privacy.mobile.analytics.event.PlaylistsTabEvent
import timber.log.Timber
import javax.inject.Inject

@OptIn(ExperimentalMaterialNavigationApi::class)
@AndroidEntryPoint
class VideoSectionActivity : PasscodeActivity(), ActionNodeCallback {
    private var bottomSheetDialogFragment: BottomSheetDialogFragment? = null

    private val videoSectionViewModel: VideoSectionViewModel by viewModels()
    private val sortByHeaderViewModel: SortByHeaderViewModel by viewModels()
    private val nodeActionsViewModel: NodeActionsViewModel by viewModels()

    @Inject
    lateinit var passcodeCryptObjectFactory: PasscodeCryptObjectFactory

    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    /**
     * File type icon mapper
     */
    @Inject
    lateinit var fileTypeIconMapper: FileTypeIconMapper

    /**
     * Mega Navigator
     */
    @Inject
    lateinit var megaNavigator: MegaNavigator

    /**
     * Mapper to convert list to json for sending data in navigation
     */
    @Inject
    lateinit var listToStringWithDelimitersMapper: ListToStringWithDelimitersMapper

    private val snackbarHostState = SnackbarHostState()

    private val videoToPlaylistActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
                RESULT_OK -> {
                    result.data?.getStringArrayListExtra(
                        VideoToPlaylistActivity.INTENT_SUCCEED_ADDED_PLAYLIST_TITLES
                    )?.let { titles ->
                        videoSectionViewModel.updateAddToPlaylistTitles(titles)
                    }
                }
            }
        }

    private val nameCollisionActivityLauncher = registerForActivityResult(
        NameCollisionActivityContract()
    ) { result ->
        if (result != null) {
            lifecycleScope.launch {
                snackbarHostState.showAutoDurationSnackbar(result)
            }
        }
    }

    private val videoSelectedActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
                RESULT_OK -> {
                    result.data?.getStringArrayListExtra(
                        VideoSelectedActivity.INTENT_KEY_VIDEO_SELECTED
                    )?.let { items ->
                        videoSectionViewModel.state.value.currentVideoPlaylist?.let { currentPlaylist ->
                            videoSectionViewModel.addVideosToPlaylist(
                                currentPlaylist.id,
                                items.map { NodeId(it.toLong()) })
                        }
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val bottomSheetActionHandler = NodeActionHandler(this, nodeActionsViewModel)

        setContent {
            val mode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            var passcodeEnabled by remember { mutableStateOf(true) }
            val scaffoldState = rememberScaffoldState(snackbarHostState = snackbarHostState)
            val nodeActionState by nodeActionsViewModel.state.collectAsStateWithLifecycle()
            val bottomSheetNavigator = rememberBottomSheetNavigator()
            val navHostController = rememberNavController(bottomSheetNavigator)

            val containers: List<@Composable (@Composable () -> Unit) -> Unit> = listOf(
                { OriginalTheme(isDark = mode.isDarkMode(), content = it) },
                {
                    PasscodeContainer(
                        passcodeCryptObjectFactory = passcodeCryptObjectFactory,
                        canLock = { passcodeEnabled },
                        content = it
                    )
                },
                { PsaContainer(content = it) }
            )

            AppContainer(
                containers = containers
            ) {
                OriginalTheme(isDark = mode.isDarkMode()) {
                    MegaScaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .systemBarsPadding()
                            .imePadding()
                            .semantics { testTagsAsResourceId = true },
                        scaffoldState = scaffoldState,
                        floatingActionButton = {
                            TransfersWidget(
                                modifier = Modifier
                                    .navigationBarsPadding()
                                    .testTag(SEARCH_SCREEN_TRANSFERS_WIDGET_TEST_TAG)
                            )
                        },
                    ) { padding ->
                        ConstraintLayout(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                        ) {
                            val (audioPlayer, videoSectionFeatureScreen) = createRefs()

                            MiniAudioPlayerView(
                                modifier = Modifier
                                    .constrainAs(audioPlayer) {
                                        bottom.linkTo(parent.bottom)
                                    }
                                    .fillMaxWidth(),
                                lifecycle = lifecycle,
                            )

                            VideoSectionScreen(
                                modifier = Modifier
                                    .constrainAs(videoSectionFeatureScreen) {
                                        top.linkTo(parent.top)
                                        bottom.linkTo(audioPlayer.top)
                                        height = Dimension.fillToConstraints
                                    }
                                    .fillMaxWidth(),
                                onSortOrderClick = { showNewSortByPanel() },
                                videoSectionViewModel = videoSectionViewModel,
                                onMenuClick = { item, mode ->
                                    // Will be implemented in ticket AND-21815
                                },
                                onMenuAction = { action ->
                                    when (action) {
                                        VideoSectionMenuAction.VideoSectionShareAction ->
                                            lifecycleScope.launch {
                                                MegaNodeUtil.shareNodes(
                                                    this@VideoSectionActivity,
                                                    videoSectionViewModel.getSelectedMegaNode()
                                                )
                                                videoSectionViewModel.clearAllSelectedVideosOfPlaylist()
                                            }

                                        VideoSectionMenuAction.VideoSectionSortByAction -> {
                                            showNewSortByPanel()
                                        }

                                        else -> return@VideoSectionScreen
                                    }
                                },
                                retryActionCallback = {
                                    videoSectionViewModel.state.value.addToPlaylistHandle?.let {
                                        launchAddToPlaylistActivity(it)
                                    }
                                },
                                scaffoldState = scaffoldState,
                                fileTypeIconMapper = fileTypeIconMapper,
                                listToStringWithDelimitersMapper = listToStringWithDelimitersMapper,
                                nodeActionHandler = bottomSheetActionHandler,
                                navHostController = navHostController,
                                bottomSheetNavigator = bottomSheetNavigator
                            )
                        }

                        StartTransferComponent(
                            event = nodeActionState.downloadEvent,
                            onConsumeEvent = nodeActionsViewModel::markDownloadEventConsumed,
                            snackBarHostState = snackbarHostState,
                        )
                    }

                    EventEffect(
                        event = nodeActionState.nodeNameCollisionsResult,
                        onConsumed = nodeActionsViewModel::markHandleNodeNameCollisionResult,
                        action = {
                            handleNodesNameCollisionResult(it)
                        }
                    )
                    EventEffect(
                        event = nodeActionState.showForeignNodeDialog,
                        onConsumed = nodeActionsViewModel::markForeignNodeDialogShown,
                        action = { navHostController.navigate(searchForeignNodeDialog) }
                    )
                    EventEffect(
                        event = nodeActionState.showQuotaDialog,
                        onConsumed = nodeActionsViewModel::markQuotaDialogShown,
                        action = {
                            navHostController.navigate(searchOverQuotaDialog.plus("/${it}"))
                        }
                    )
                    EventEffect(
                        event = nodeActionState.contactsData,
                        onConsumed = nodeActionsViewModel::markShareFolderAccessDialogShown,
                        action = { (contactData, isFromBackups, nodeHandles) ->
                            val contactList =
                                contactData.joinToString(separator = contactArraySeparator)
                            navHostController.navigate(
                                shareFolderAccessDialog.plus("/${contactList}")
                                    .plus("/${isFromBackups}")
                                    .plus("/${nodeHandles}")
                            )
                        },
                    )
                    EventEffect(
                        event = nodeActionState.selectAll,
                        onConsumed = nodeActionsViewModel::selectAllConsumed,
                        action = videoSectionViewModel::selectAllNodes
                    )
                    EventEffect(
                        event = nodeActionState.clearAll,
                        onConsumed = nodeActionsViewModel::clearAllConsumed,
                        action = videoSectionViewModel::clearAllSelectedVideos
                    )
                    EventEffect(
                        event = nodeActionState.infoToShowEvent,
                        onConsumed = nodeActionsViewModel::onInfoToShowEventConsumed,
                    ) { info ->
                        info?.let {
                            info.getInfo(this@VideoSectionActivity).let { text ->
                                scaffoldState.snackbarHostState.showAutoDurationSnackbar(text)
                            }
                        } ?: findActivity()?.finish()
                    }
                }
            }
        }
        setupCollectFlow()
    }

    /**
     * onViewCreated
     */
    @OptIn(FlowPreview::class)
    private fun setupCollectFlow() {
        collectFlow(sortByHeaderViewModel.orderChangeState) {
            videoSectionViewModel.refreshWhenOrderChanged()
        }

        collectFlow(
            videoSectionViewModel.state.map { it.isPendingRefresh }.debounce(500L)
                .distinctUntilChanged()
        ) { isPendingRefresh ->
            if (isPendingRefresh) {
                with(videoSectionViewModel) {
                    refreshNodes()
                    refreshRecentlyWatchedVideos()
                    markHandledPendingRefresh()
                }
            }
        }

        collectFlow(
            videoSectionViewModel.state.map { it.isVideoPlaylistCreatedSuccessfully }
                .distinctUntilChanged()
        ) { isSuccess ->
            if (isSuccess) {
                navigateToVideoSelectedActivity()
            }
        }

        collectFlow(
            videoSectionViewModel.state.map { it.clickedItem }.distinctUntilChanged()
        ) { fileNode ->
            fileNode?.let {
                openVideoFileFromAllVideos(it)
            }
        }

        collectFlow(
            videoSectionViewModel.state.map { it.clickedPlaylistDetailItem }.distinctUntilChanged()
        ) { fileNode ->
            fileNode?.let {
                openVideoFileFromPlaylist(it)
            }
        }

        collectFlow(
            videoSectionViewModel.state.map { it.isLaunchVideoToPlaylistActivity }
                .distinctUntilChanged()
        ) { isLaunch ->
            if (isLaunch) {
                videoSectionViewModel.state.value.addToPlaylistHandle?.let {
                    launchAddToPlaylistActivity(it)
                }
                videoSectionViewModel.resetIsLaunchVideoToPlaylistActivity()
            }
        }

        collectFlow(
            videoSectionViewModel.tabState.map { it.selectedTab }.distinctUntilChanged()
        ) { tab ->
            when (tab) {
                VideoSectionTab.All -> Analytics.tracker.trackEvent(AllVideosTabEvent)
                VideoSectionTab.Playlists -> Analytics.tracker.trackEvent(PlaylistsTabEvent)
            }
        }

        collectFlow(
            videoSectionViewModel.state.map { it.navigateToVideoSelected }.distinctUntilChanged()
        ) {
            if (it) {
                if (getStorageState() == StorageState.PayWall) {
                    showOverDiskQuotaPaywallWarning()
                } else {
                    navigateToVideoSelectedActivity()
                }
                videoSectionViewModel.updateNavigateToVideoSelected(false)
            }
        }
    }

    private fun navigateToVideoSelectedActivity() {
        videoSelectedActivityLauncher.launch(
            Intent(this, VideoSelectedActivity::class.java)
        )
    }

    private fun showNewSortByPanel() {
        val currentSelectTab = videoSectionViewModel.tabState.value.selectedTab
        val orderType = when {
            currentSelectTab == VideoSectionTab.All -> ORDER_CLOUD
            videoSectionViewModel.state.value.currentVideoPlaylist?.isSystemVideoPlayer == true ->
                ORDER_FAVOURITES

            else -> ORDER_VIDEO_PLAYLIST
        }

        if (bottomSheetDialogFragment.isBottomSheetDialogShown()) {
            return
        }
        bottomSheetDialogFragment = SortByBottomSheetDialogFragment.newInstance(orderType)
        bottomSheetDialogFragment?.show(
            supportFragmentManager,
            bottomSheetDialogFragment?.tag
        )
    }

    private fun launchAddToPlaylistActivity(videoHandle: Long) {
        videoToPlaylistActivityLauncher.launch(
            Intent(this, VideoToPlaylistActivity::class.java).apply {
                putExtra(INTENT_EXTRA_KEY_HANDLE, videoHandle)
            }
        )
    }

    private fun handleNodesNameCollisionResult(result: NodeNameCollisionsResult) {
        if (result.conflictNodes.isNotEmpty()) {
            nameCollisionActivityLauncher
                .launch(result.conflictNodes.values.toCollection(ArrayList()))
        }
        if (result.noConflictNodes.isNotEmpty()) {
            when (result.type) {
                NodeNameCollisionType.MOVE -> nodeActionsViewModel.moveNodes(result.noConflictNodes)
                NodeNameCollisionType.COPY -> nodeActionsViewModel.copyNodes(result.noConflictNodes)
                else -> Timber.d("Not implemented")
            }
        }
    }

    private fun openVideoFileFromAllVideos(node: TypedFileNode) {
        val uiState = videoSectionViewModel.state.value
        val isDurationFilterEnabled =
            uiState.durationSelectedFilterOption != DurationFilterOption.AllDurations
        val isLocationFilterEnabled =
            uiState.locationSelectedFilterOption != LocationFilterOption.AllLocations
        val isSearchMode = uiState.searchState == SearchWidgetState.EXPANDED
                || isDurationFilterEnabled || isLocationFilterEnabled

        lifecycleScope.launch {
            runCatching {
                val nodeContentUri = videoSectionViewModel.getNodeContentUri(node)
                megaNavigator.openMediaPlayerActivityByFileNode(
                    context = this@VideoSectionActivity,
                    contentUri = nodeContentUri,
                    fileNode = node,
                    sortOrder = sortByHeaderViewModel.cloudSortOrder.value,
                    viewType = if (isSearchMode) {
                        SEARCH_BY_ADAPTER
                    } else {
                        VIDEO_BROWSE_ADAPTER
                    },
                    isFolderLink = false,
                    searchedItems =
                        if (uiState.currentDestinationRoute == videoSectionRoute && isSearchMode) {
                            uiState.allVideos.map { it.id.longValue }
                        } else {
                            null
                        },
                )
                videoSectionViewModel.updateClickedItem(null)
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private fun openVideoFileFromPlaylist(node: TypedVideoNode) {
        val uiState = videoSectionViewModel.state.value
        lifecycleScope.launch {
            runCatching {
                val nodeContentUri = videoSectionViewModel.getNodeContentUri(node)
                megaNavigator.openMediaPlayerActivityByFileNode(
                    context = this@VideoSectionActivity,
                    contentUri = nodeContentUri,
                    fileNode = node,
                    sortOrder = sortByHeaderViewModel.cloudSortOrder.value,
                    viewType = SEARCH_BY_ADAPTER,
                    isFolderLink = false,
                    searchedItems = uiState.currentVideoPlaylist?.videos?.map { it.id.longValue },
                    mediaQueueTitle = uiState.currentVideoPlaylist?.title,
                    collectionId = uiState.currentVideoPlaylist?.id?.longValue
                )
                videoSectionViewModel.updateClickedPlaylistDetailItem(null)
            }.onFailure {
                Timber.e(it)
            }
        }
    }
}