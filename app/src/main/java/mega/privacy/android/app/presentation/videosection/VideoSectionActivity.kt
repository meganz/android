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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.interfaces.ActionNodeCallback
import mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown
import mega.privacy.android.app.modalbottomsheet.SortByBottomSheetDialogFragment
import mega.privacy.android.app.presentation.container.AppContainer
import mega.privacy.android.app.presentation.extensions.getStorageState
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.app.presentation.psa.PsaContainer
import mega.privacy.android.app.presentation.search.view.MiniAudioPlayerView
import mega.privacy.android.app.presentation.security.check.PasscodeContainer
import mega.privacy.android.app.presentation.videosection.model.DurationFilterOption
import mega.privacy.android.app.presentation.videosection.model.LocationFilterOption
import mega.privacy.android.app.presentation.videosection.model.VideoSectionTab
import mega.privacy.android.app.presentation.videosection.view.VideoSectionFeatureScreen
import mega.privacy.android.app.presentation.videosection.view.videoSectionRoute
import mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.app.utils.Constants.ORDER_CLOUD
import mega.privacy.android.app.utils.Constants.ORDER_FAVOURITES
import mega.privacy.android.app.utils.Constants.ORDER_VIDEO_PLAYLIST
import mega.privacy.android.app.utils.Constants.SEARCH_BY_ADAPTER
import mega.privacy.android.app.utils.Constants.VIDEO_BROWSE_ADAPTER
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.legacy.core.ui.model.SearchWidgetState
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
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

    @Inject
    lateinit var passcodeCryptObjectFactory: PasscodeCryptObjectFactory

    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    @Inject
    lateinit var megaNavigator: MegaNavigator

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
        setContent {
            val mode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            var passcodeEnabled by remember { mutableStateOf(true) }
            val scaffoldState = rememberScaffoldState()

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
                        scaffoldState = scaffoldState
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

                            VideoSectionFeatureScreen(
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
                                onMenuAction = {
                                    // Will be implemented in ticket AND-21804
                                },
                                retryActionCallback = {
                                    videoSectionViewModel.state.value.addToPlaylistHandle?.let {
                                        launchAddToPlaylistActivity(it)
                                    }
                                }
                            )
                        }
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