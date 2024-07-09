package mega.privacy.android.app.presentation.videosection

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.fragments.homepage.HomepageSearchable
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.bottomsheet.NodeOptionsBottomSheetDialogFragment
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.hidenode.HiddenNodesOnboardingActivity
import mega.privacy.android.app.presentation.mapper.GetOptionsForToolbarMapper
import mega.privacy.android.app.presentation.search.view.MiniAudioPlayerView
import mega.privacy.android.app.presentation.videosection.model.DurationFilterOption
import mega.privacy.android.app.presentation.videosection.model.LocationFilterOption
import mega.privacy.android.app.presentation.videosection.model.VideoSectionTab
import mega.privacy.android.app.presentation.videosection.model.VideoUIEntity
import mega.privacy.android.app.presentation.videosection.view.VideoSectionFeatureScreen
import mega.privacy.android.app.presentation.videosection.view.playlist.videoPlaylistDetailRoute
import mega.privacy.android.app.presentation.videosection.view.videoSectionRoute
import mega.privacy.android.app.utils.Constants.ORDER_CLOUD
import mega.privacy.android.app.utils.Constants.ORDER_VIDEO_PLAYLIST
import mega.privacy.android.app.utils.Constants.SEARCH_BY_ADAPTER
import mega.privacy.android.app.utils.Constants.VIDEO_BROWSE_ADAPTER
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.callManager
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import timber.log.Timber
import javax.inject.Inject

/**
 * The Fragment for video section
 */
@AndroidEntryPoint
class VideoSectionFragment : Fragment(), HomepageSearchable {

    private val videoSectionViewModel by viewModels<VideoSectionViewModel>()
    private val sortByHeaderViewModel: SortByHeaderViewModel by activityViewModels()

    /**
     * Application Theme Mode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    /**
     * Mapper to get options for Action Bar
     */
    @Inject
    lateinit var getOptionsForToolbarMapper: GetOptionsForToolbarMapper

    /**
     * Mega Navigator
     */
    @Inject
    lateinit var megaNavigator: MegaNavigator

    private var actionMode: ActionMode? = null

    private val playlistMenuMoreProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.fragment_playlist_menu_more, menu)
            menu.findItem(R.id.menu_playlist_more).setOnMenuItemClickListener {
                videoSectionViewModel.setShouldShowMoreVideoPlaylistOptions(true)
                true
            }
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean = true
    }

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
            val uiState by videoSectionViewModel.state.collectAsStateWithLifecycle()
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
                        onLongClick = { item, index ->
                            activateActionMode()
                            videoSectionViewModel.onItemLongClicked(item, index)
                        },
                        onMenuClick = { item ->
                            showOptionsMenuForItem(item)
                        },
                        onAddElementsClicked = {
                            navigateToVideoSelectedActivity()
                        },
                        onPlaylistItemLongClick = { item, index ->
                            activateVideoPlaylistActionMode(ACTION_TYPE_VIDEO_PLAYLIST)
                            videoSectionViewModel.onVideoPlaylistItemClicked(item, index)
                        },
                        onPlaylistDetailItemLongClick = { item, index ->
                            activateVideoPlaylistActionMode(ACTION_TYPE_VIDEO_PLAYLIST_DETAIL)
                            videoSectionViewModel.onVideoItemOfPlaylistLongClicked(item, index)
                        },
                        onActionModeFinished = { actionMode?.finish() }
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

    private fun activateActionMode() {
        if (actionMode == null) {
            actionMode =
                (requireActivity() as? AppCompatActivity)?.startSupportActionMode(
                    VideoSectionActionModeCallback(
                        fragment = this,
                        managerActivity = requireActivity() as ManagerActivity,
                        childFragmentManager = childFragmentManager,
                        videoSectionViewModel = videoSectionViewModel,
                        getOptionsForToolbarMapper = getOptionsForToolbarMapper
                    ) {
                        disableSelectMode()
                    }
                )
            videoSectionViewModel.setActionMode(true)
        }
    }

    private fun activateVideoPlaylistActionMode(actionType: Int) {
        if (actionMode == null) {
            actionMode =
                (requireActivity() as? AppCompatActivity)?.startSupportActionMode(
                    VideoPlaylistActionMode(
                        managerActivity = requireActivity() as ManagerActivity,
                        videoSectionViewModel = videoSectionViewModel,
                        actionType = actionType
                    ) {
                        disableSelectMode()
                    }
                )
            videoSectionViewModel.setActionMode(true)
        }
    }

    /**
     * onViewCreated
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewLifecycleOwner.collectFlow(sortByHeaderViewModel.orderChangeState) {
            videoSectionViewModel.refreshWhenOrderChanged()
        }

        viewLifecycleOwner.collectFlow(
            videoSectionViewModel.state.map { it.isPendingRefresh }.distinctUntilChanged()
        ) { isPendingRefresh ->
            if (isPendingRefresh) {
                with(videoSectionViewModel) {
                    refreshNodes()
                    markHandledPendingRefresh()
                }
            }
        }

        viewLifecycleOwner.collectFlow(
            videoSectionViewModel.state.map { it.allVideos }.distinctUntilChanged()
        ) { list ->
            if (!videoSectionViewModel.state.value.searchMode && list.isNotEmpty()) {
                callManager {
                    it.invalidateOptionsMenu()
                }
            }
        }

        viewLifecycleOwner.collectFlow(
            videoSectionViewModel.state.map { it.searchMode }.distinctUntilChanged()
        ) { isSearchMode ->
            if (!isSearchMode) {
                (activity as? ManagerActivity)?.closeSearchView()
            }
        }

        viewLifecycleOwner.collectFlow(
            videoSectionViewModel.state.map { it.currentDestinationRoute }.distinctUntilChanged(),
            minActiveState = Lifecycle.State.CREATED
        ) { route ->
            route?.let { updateToolbarWhenDestinationChanged(isAddMenu = true) }
        }

        viewLifecycleOwner.collectFlow(
            videoSectionViewModel.state.map { it.updateToolbarTitle }.distinctUntilChanged(),
        ) { title ->
            updateToolbarWhenDestinationChanged(title = title)
        }

        viewLifecycleOwner.collectFlow(
            videoSectionViewModel.state.map { it.actionMode }.distinctUntilChanged()
        ) { isActionMode ->
            if (!isActionMode) {
                actionMode?.finish()
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

        viewLifecycleOwner.lifecycleScope.launch {
            merge(
                videoSectionViewModel.state.map { it.selectedVideoHandles }.distinctUntilChanged(),
                videoSectionViewModel.state.map { it.selectedVideoPlaylistHandles }
                    .distinctUntilChanged(),
                videoSectionViewModel.state.map { it.selectedVideoElementIDs }
                    .distinctUntilChanged()
            ).collectLatest { list ->
                updateActionModeTitle(count = list.size)
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
        val isSearchMode = uiState.searchMode || isDurationFilterEnabled || isLocationFilterEnabled

        viewLifecycleOwner.lifecycleScope.launch {
            val nodeContentUri = videoSectionViewModel.getNodeContentUri(node) ?: return@launch
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
        }
    }

    private fun openVideoFileFromPlaylist(node: TypedVideoNode) {
        val uiState = videoSectionViewModel.state.value
        viewLifecycleOwner.lifecycleScope.launch {
            val nodeContentUri = videoSectionViewModel.getNodeContentUri(node) ?: return@launch
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
        }
    }

    private fun updateActionModeTitle(count: Int) {
        if (count == 0) actionMode?.finish()
        actionMode?.title = count.toString()

        runCatching {
            actionMode?.invalidate()
        }.onFailure {
            Timber.e(it, "Invalidate error")
        }
    }

    private fun showOptionsMenuForItem(item: VideoUIEntity) {
        (requireActivity() as ManagerActivity).showNodeOptionsPanel(
            nodeId = item.id,
            mode = NodeOptionsBottomSheetDialogFragment.CLOUD_DRIVE_MODE
        )
    }

    private fun updateToolbarWhenDestinationChanged(
        title: String? = null,
        isAddMenu: Boolean = false,
    ) {
        val route = videoSectionViewModel.state.value.currentDestinationRoute ?: return
        (activity as? ManagerActivity)?.let { managerActivity ->
            when (route) {
                videoSectionRoute -> {
                    managerActivity.setToolbarTitle(getString(R.string.sortby_type_video_first))
                    managerActivity.removeMenuProvider(playlistMenuMoreProvider)
                }

                videoPlaylistDetailRoute -> {
                    if (isAddMenu) {
                        managerActivity.addMenuProvider(playlistMenuMoreProvider)
                    }
                    managerActivity.setToolbarTitle(title ?: "")
                }
            }
            managerActivity.invalidateOptionsMenu()
        }
    }

    private fun disableSelectMode() {
        actionMode = null
        videoSectionViewModel.clearAllSelectedVideos()
        videoSectionViewModel.clearAllSelectedVideoPlaylists()
        videoSectionViewModel.clearAllSelectedVideosOfPlaylist()
        videoSectionViewModel.setActionMode(false)
    }

    /**
     * Should show search menu
     *
     * @return true if should show search menu, false otherwise
     */
    override fun shouldShowSearchMenu(): Boolean = videoSectionViewModel.shouldShowSearchMenu()

    /**
     * Search ready
     */
    override fun searchReady() {
        videoSectionViewModel.searchReady()
    }

    /**
     * Search query
     *
     * @param query query string
     */
    override fun searchQuery(query: String) {
        videoSectionViewModel.searchQuery(query)
    }

    /**
     * Exit search
     */
    override fun exitSearch() {
        videoSectionViewModel.exitSearch()
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

    companion object {
        /**
         * The action type for video playlist
         */
        const val ACTION_TYPE_VIDEO_PLAYLIST = 10

        /**
         * The action type for video playlist detail
         */
        const val ACTION_TYPE_VIDEO_PLAYLIST_DETAIL = 11
    }
}