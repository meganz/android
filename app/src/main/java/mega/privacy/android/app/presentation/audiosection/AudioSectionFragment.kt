package mega.privacy.android.app.presentation.audiosection

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
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
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.audiosection.model.AudioUiEntity
import mega.privacy.android.app.presentation.audiosection.view.AudioSectionComposeView
import mega.privacy.android.app.presentation.bottomsheet.NodeOptionsBottomSheetDialogFragment
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.hidenode.HiddenNodesOnboardingActivity
import mega.privacy.android.app.presentation.mapper.GetOptionsForToolbarMapper
import mega.privacy.android.app.presentation.search.view.MiniAudioPlayerView
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.AUDIO_BROWSE_ADAPTER
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.callManager
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.mobile.analytics.event.HideNodeMultiSelectMenuItemEvent
import timber.log.Timber
import javax.inject.Inject

/**
 * The fragment for audio section
 */
@AndroidEntryPoint
class AudioSectionFragment : Fragment() {
    private val audioSectionViewModel by viewModels<AudioSectionViewModel>()
    private val sortByHeaderViewModel: SortByHeaderViewModel by activityViewModels()

    /**
     * Application Theme Mode
     */
    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

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
            val themeMode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val uiState by audioSectionViewModel.state.collectAsStateWithLifecycle()
            OriginalTheme(isDark = themeMode.isDarkMode()) {
                ConstraintLayout(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val (audioPlayer, audioSectionComposeView) = createRefs()
                    MiniAudioPlayerView(
                        modifier = Modifier
                            .constrainAs(audioPlayer) {
                                bottom.linkTo(parent.bottom)
                            }
                            .fillMaxWidth(),
                        lifecycle = lifecycle,
                    )
                    AudioSectionComposeView(
                        viewModel = audioSectionViewModel,
                        modifier = Modifier
                            .constrainAs(audioSectionComposeView) {
                                top.linkTo(parent.top)
                                bottom.linkTo(audioPlayer.top)
                                height = Dimension.fillToConstraints
                            }
                            .fillMaxWidth(),
                        onChangeViewTypeClick = audioSectionViewModel::onChangeViewTypeClicked,
                        onSortOrderClick = { showSortByPanel() },
                        onLongClick = { item, index ->
                            audioSectionViewModel.onItemLongClicked(item, index)
                            activateActionMode()
                        },
                        onMenuClick = { item ->
                            showOptionsMenuForItem(item)
                        }
                    )
                }
            }
            updateActionModeTitle(count = uiState.selectedAudioHandles.size)
        }
    }

    /**
     * onViewCreated
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewLifecycleOwner.collectFlow(sortByHeaderViewModel.orderChangeState) {
            audioSectionViewModel.refreshWhenOrderChanged()
        }

        viewLifecycleOwner.collectFlow(
            audioSectionViewModel.state.map { it.isPendingRefresh }.distinctUntilChanged()
        ) { isPendingRefresh ->
            if (isPendingRefresh) {
                with(audioSectionViewModel) {
                    refreshNodes()
                    markHandledPendingRefresh()
                }
            }
        }

        viewLifecycleOwner.collectFlow(
            audioSectionViewModel.state.map { it.allAudios }.distinctUntilChanged()
        ) { list ->
            if (list.isNotEmpty()) {
                callManager {
                    it.invalidateOptionsMenu()
                }
            }
        }

        viewLifecycleOwner.collectFlow(
            audioSectionViewModel.state.map { it.clickedItem }.distinctUntilChanged()
        ) { fileNode ->
            fileNode?.let {
                val uiState = audioSectionViewModel.state.value
                viewLifecycleOwner.lifecycleScope.launch {
                    runCatching {
                        val nodeContentUri =
                            audioSectionViewModel.getNodeContentUri(it) ?: return@launch
                        megaNavigator.openMediaPlayerActivityByFileNode(
                            context = requireContext(),
                            contentUri = nodeContentUri,
                            fileNode = it,
                            sortOrder = sortByHeaderViewModel.cloudSortOrder.value,
                            viewType = AUDIO_BROWSE_ADAPTER,
                            isFolderLink = false,
                            searchedItems = uiState.allAudios.map { it.id.longValue }
                        )
                        audioSectionViewModel.updateClickedItem(null)
                    }.onFailure {
                        Timber.e(it)
                    }
                }
            }
        }
    }

    private fun showSortByPanel() {
        (requireActivity() as? ManagerActivity)?.showNewSortByPanel(Constants.ORDER_CLOUD)
    }

    private fun activateActionMode() {
        if (actionMode == null) {
            actionMode =
                (requireActivity() as? AppCompatActivity)?.startSupportActionMode(
                    AudioSectionActionModeCallback(
                        fragment = this,
                        managerActivity = requireActivity() as ManagerActivity,
                        childFragmentManager = childFragmentManager,
                        audioSectionViewModel = audioSectionViewModel,
                        getOptionsForToolbarMapper = getOptionsForToolbarMapper,
                    ) {
                        disableSelectMode()
                    }
                )
        }
    }

    private fun disableSelectMode() {
        actionMode = null
        audioSectionViewModel.clearAllSelectedAudios()
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

    private fun showOptionsMenuForItem(item: AudioUiEntity) {
        (requireActivity() as? ManagerActivity)?.showNodeOptionsPanel(
            nodeId = item.id,
            mode = NodeOptionsBottomSheetDialogFragment.CLOUD_DRIVE_MODE
        )
    }

    suspend fun handleHideNodeClick() {
        Analytics.tracker.trackEvent(HideNodeMultiSelectMenuItemEvent)
        var isPaid: Boolean
        var isHiddenNodesOnboarded: Boolean
        var isBusinessAccountExpired: Boolean
        with(audioSectionViewModel.state.value) {
            isPaid = this.accountType?.isPaid ?: false
            isHiddenNodesOnboarded = this.isHiddenNodesOnboarded
            isBusinessAccountExpired = this.isBusinessAccountExpired
        }

        if (!isPaid || isBusinessAccountExpired) {
            val intent = HiddenNodesOnboardingActivity.createScreen(
                context = requireContext(),
                isOnboarding = false,
            )
            hiddenNodesOnboardingLauncher.launch(intent)
            activity?.overridePendingTransition(0, 0)
        } else if (isHiddenNodesOnboarded) {
            val nodes = audioSectionViewModel.getSelectedNodes()
            audioSectionViewModel.hideOrUnhideNodes(
                nodeIds = nodes.map { it.id },
                hide = true,
            )
            val message =
                resources.getQuantityString(
                    R.plurals.hidden_nodes_result_message,
                    nodes.size,
                    nodes.size,
                )
            Util.showSnackbar(requireActivity(), message)
        } else {
            tempNodeIds = audioSectionViewModel.getSelectedNodes().map { it.id }
            showHiddenNodesOnboarding()
        }
    }

    private fun showHiddenNodesOnboarding() {
        audioSectionViewModel.setHiddenNodesOnboarded()

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

        audioSectionViewModel.hideOrUnhideNodes(
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