package mega.privacy.android.app.presentation.videosection

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.main.dialog.removelink.RemovePublicLinkDialogFragment
import mega.privacy.android.app.main.dialog.rubbishbin.ConfirmMoveToRubbishBinDialogFragment
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.hidenode.HiddenNodesOnboardingActivity
import mega.privacy.android.app.presentation.search.view.MiniAudioPlayerView
import mega.privacy.android.app.presentation.videosection.model.DurationFilterOption
import mega.privacy.android.app.presentation.videosection.model.LocationFilterOption
import mega.privacy.android.app.presentation.videosection.model.VideoSectionMenuAction
import mega.privacy.android.app.presentation.videosection.model.VideoSectionTab
import mega.privacy.android.app.presentation.videosection.model.VideoUIEntity
import mega.privacy.android.app.presentation.videosection.view.VideoSectionFeatureScreen
import mega.privacy.android.app.presentation.videosection.view.recentlywatched.videoRecentlyWatchedRoute
import mega.privacy.android.app.presentation.videosection.view.videoSectionRoute
import mega.privacy.android.app.utils.Constants.ORDER_CLOUD
import mega.privacy.android.app.utils.Constants.ORDER_VIDEO_PLAYLIST
import mega.privacy.android.app.utils.Constants.SEARCH_BY_ADAPTER
import mega.privacy.android.app.utils.Constants.VIDEO_BROWSE_ADAPTER
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.legacy.core.ui.model.SearchWidgetState
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import timber.log.Timber
import javax.inject.Inject

/**
 * The Fragment for video section
 */
@AndroidEntryPoint
class VideoSectionFragment : Fragment() {

    private val videoSectionViewModel: VideoSectionViewModel by activityViewModels()
    private val sortByHeaderViewModel: SortByHeaderViewModel by activityViewModels()

    /**
     * Application Theme Mode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    /**
     * Mega Navigator
     */
    @Inject
    lateinit var megaNavigator: MegaNavigator

    private val videoSelectedActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
                Activity.RESULT_OK -> {
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

    private var tempNodeIds: List<NodeId> = listOf()

    /**
     * onCreateView
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            val themeMode by getThemeMode()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            OriginalTempTheme(isDark = themeMode.isDarkMode()) {
                ConstraintLayout(
                    modifier = Modifier.fillMaxSize()
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
                        onSortOrderClick = { showSortByPanel() },
                        videoSectionViewModel = videoSectionViewModel,
                        onMenuClick = ::showOptionsMenuForItem,
                        onAddElementsClicked = {
                            navigateToVideoSelectedActivity()
                        },
                        onMenuAction = ::handleVideoSectionMenuAction
                    )
                }
            }
        }
    }

    private fun navigateToVideoSelectedActivity() {
        videoSelectedActivityLauncher.launch(
            Intent(
                requireActivity(),
                VideoSelectedActivity::class.java
            )
        )
    }

    private fun showSortByPanel() {
        val currentSelectTab = videoSectionViewModel.tabState.value.selectedTab
        (requireActivity() as ManagerActivity).showNewSortByPanel(
            if (currentSelectTab == VideoSectionTab.All) ORDER_CLOUD else ORDER_VIDEO_PLAYLIST
        )
    }

    private fun handleVideoSectionMenuAction(action: VideoSectionMenuAction?) =
        (activity as? ManagerActivity)?.let { managerActivity ->
            viewLifecycleOwner.lifecycleScope.launch {
                val selectedVideos = videoSectionViewModel.state.value.selectedVideoHandles
                when (action) {
                    is VideoSectionMenuAction.VideoSectionDownloadAction ->
                        managerActivity.saveNodesToDevice(
                            nodes = videoSectionViewModel.getSelectedMegaNode(),
                            highPriority = false,
                            isFolderLink = false,
                            fromChat = false,
                        )

                    is VideoSectionMenuAction.VideoSectionRenameAction ->
                        managerActivity.showRenameDialog(videoSectionViewModel.getSelectedMegaNode()[0])

                    is VideoSectionMenuAction.VideoSectionShareAction ->
                        MegaNodeUtil.shareNodes(
                            managerActivity,
                            videoSectionViewModel.getSelectedMegaNode()
                        )

                    is VideoSectionMenuAction.VideoSectionGetLinkAction ->
                        managerActivity.showGetLinkActivity(videoSectionViewModel.getSelectedMegaNode())

                    is VideoSectionMenuAction.VideoSectionRemoveLinkAction ->
                        RemovePublicLinkDialogFragment.newInstance(
                            videoSectionViewModel.getSelectedNodes()
                                .map { node -> node.id.longValue })
                            .show(childFragmentManager, RemovePublicLinkDialogFragment.TAG)

                    is VideoSectionMenuAction.VideoSectionSendToChatAction ->
                        managerActivity.attachNodesToChats(videoSectionViewModel.getSelectedMegaNode())

                    is VideoSectionMenuAction.VideoSectionRubbishBinAction ->
                        selectedVideos.takeIf { handles ->
                            handles.isNotEmpty()
                        }?.let { handles ->
                            ConfirmMoveToRubbishBinDialogFragment.newInstance(handles)
                                .show(
                                    managerActivity.supportFragmentManager,
                                    ConfirmMoveToRubbishBinDialogFragment.TAG
                                )
                        }

                    is VideoSectionMenuAction.VideoSectionHideAction -> handleHideNodeClick()

                    is VideoSectionMenuAction.VideoSectionUnhideAction -> videoSectionViewModel.unhideNodes()

                    is VideoSectionMenuAction.VideoSectionCopyAction ->
                        NodeController(managerActivity).chooseLocationToCopyNodes(selectedVideos)

                    is VideoSectionMenuAction.VideoSectionMoveAction ->
                        NodeController(managerActivity).chooseLocationToMoveNodes(selectedVideos)

                    else -> {}
                }
                videoSectionViewModel.clearAllSelectedVideos()
                videoSectionViewModel.clearAllSelectedVideoPlaylists()
                videoSectionViewModel.clearAllSelectedVideosOfPlaylist()
            }
        }

    /**
     * onViewCreated
     */
    @OptIn(FlowPreview::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? ManagerActivity)?.let { managerActivity ->
            managerActivity.supportActionBar?.hide()
        }
        viewLifecycleOwner.collectFlow(sortByHeaderViewModel.orderChangeState) {
            videoSectionViewModel.refreshWhenOrderChanged()
        }

        viewLifecycleOwner.collectFlow(
            videoSectionViewModel.state.map { it.isPendingRefresh }.debounce(500L)
                .distinctUntilChanged()
        ) { isPendingRefresh ->
            if (isPendingRefresh) {
                with(videoSectionViewModel) {
                    refreshNodes()
                    if (state.value.currentDestinationRoute == videoRecentlyWatchedRoute) {
                        loadRecentlyWatchedVideos()
                    }
                    markHandledPendingRefresh()
                }
            }
        }

        viewLifecycleOwner.collectFlow(
            videoSectionViewModel.state.map { it.isVideoPlaylistCreatedSuccessfully }
                .distinctUntilChanged()
        ) { isSuccess ->
            if (isSuccess) {
                navigateToVideoSelectedActivity()
            }
        }

        viewLifecycleOwner.collectFlow(
            videoSectionViewModel.state.map { it.clickedItem }.distinctUntilChanged()
        ) { fileNode ->
            fileNode?.let {
                openVideoFileFromAllVideos(it)
            }
        }

        viewLifecycleOwner.collectFlow(
            videoSectionViewModel.state.map { it.clickedPlaylistDetailItem }.distinctUntilChanged()
        ) { fileNode ->
            fileNode?.let {
                openVideoFileFromPlaylist(it)
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

        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                val nodeContentUri = videoSectionViewModel.getNodeContentUri(node)
                megaNavigator.openMediaPlayerActivityByFileNode(
                    context = requireContext(),
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
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                val nodeContentUri = videoSectionViewModel.getNodeContentUri(node)
                megaNavigator.openMediaPlayerActivityByFileNode(
                    context = requireContext(),
                    contentUri = nodeContentUri,
                    fileNode = node,
                    sortOrder = sortByHeaderViewModel.cloudSortOrder.value,
                    viewType = SEARCH_BY_ADAPTER,
                    isFolderLink = false,
                    searchedItems = uiState.currentVideoPlaylist?.videos?.map { it.id.longValue },
                    mediaQueueTitle = uiState.currentVideoPlaylist?.title
                )
                videoSectionViewModel.updateClickedPlaylistDetailItem(null)
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private fun showOptionsMenuForItem(item: VideoUIEntity, mode: Int) {
        (requireActivity() as ManagerActivity).showNodeOptionsPanel(nodeId = item.id, mode = mode)
    }

    suspend fun handleHideNodeClick() {
        var isPaid: Boolean
        var isHiddenNodesOnboarded: Boolean
        with(videoSectionViewModel.state.value) {
            isPaid = this.accountDetail?.levelDetail?.accountType?.isPaid ?: false
            isHiddenNodesOnboarded = this.isHiddenNodesOnboarded
        }

        if (!isPaid) {
            val intent = HiddenNodesOnboardingActivity.createScreen(
                context = requireContext(),
                isOnboarding = false,
            )
            hiddenNodesOnboardingLauncher.launch(intent)
            activity?.overridePendingTransition(0, 0)
        } else if (isHiddenNodesOnboarded) {
            videoSectionViewModel.hideOrUnhideNodes(
                nodeIds = videoSectionViewModel.getSelectedNodes().map { it.id },
                hide = true,
            )
        } else {
            tempNodeIds = videoSectionViewModel.getSelectedNodes().map { it.id }
            showHiddenNodesOnboarding()
        }
    }

    private fun showHiddenNodesOnboarding() {
        videoSectionViewModel.setHiddenNodesOnboarded()

        val intent = HiddenNodesOnboardingActivity.createScreen(
            context = requireContext(),
            isOnboarding = true,
        )
        hiddenNodesOnboardingLauncher.launch(intent)
        activity?.overridePendingTransition(0, 0)
    }

    private val hiddenNodesOnboardingLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            ::handleHiddenNodesOnboardingResult,
        )

    private fun handleHiddenNodesOnboardingResult(result: ActivityResult) {
        if (result.resultCode != Activity.RESULT_OK) return

        videoSectionViewModel.hideOrUnhideNodes(
            nodeIds = tempNodeIds,
            hide = true,
        )

        val message =
            resources.getQuantityString(
                R.plurals.hidden_nodes_result_message,
                tempNodeIds.size,
                tempNodeIds.size,
            )
        Util.showSnackbar(requireActivity(), message)
    }
}