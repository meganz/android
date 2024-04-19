package mega.privacy.android.app.presentation.audiosection

import android.app.Activity
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
import android.net.Uri
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
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.fragments.homepage.EventObserver
import mega.privacy.android.app.fragments.homepage.HomepageSearchable
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.audiosection.model.AudioUiEntity
import mega.privacy.android.app.presentation.bottomsheet.NodeOptionsBottomSheetDialogFragment
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.hidenode.HiddenNodesOnboardingActivity
import mega.privacy.android.app.presentation.mapper.GetOptionsForToolbarMapper
import mega.privacy.android.app.presentation.search.view.MiniAudioPlayerView
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.AUDIO_BROWSE_ADAPTER
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FILE_NAME
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ORDER_GET_CHILDREN
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_POSITION
import mega.privacy.android.app.utils.Constants.SEARCH_BY_ADAPTER
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.Util.getMediaIntent
import mega.privacy.android.app.utils.callManager
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.theme.MegaAppTheme
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * The fragment for audio section
 */
@AndroidEntryPoint
class AudioSectionFragment : Fragment(), HomepageSearchable {
    private val audioSectionViewModel by viewModels<AudioSectionViewModel>()
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
            val themeMode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val uiState by audioSectionViewModel.state.collectAsStateWithLifecycle()
            MegaAppTheme(isDark = themeMode.isDarkMode()) {
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
                        uiState = uiState,
                        modifier = Modifier
                            .constrainAs(audioSectionComposeView) {
                                top.linkTo(parent.top)
                                bottom.linkTo(audioPlayer.top)
                                height = Dimension.fillToConstraints
                            }
                            .fillMaxWidth(),
                        onChangeViewTypeClick = audioSectionViewModel::onChangeViewTypeClicked,
                        onSortOrderClick = { showSortByPanel() },
                        onClick = { item, index ->
                            if (uiState.isInSelection) {
                                audioSectionViewModel.onItemClicked(item, index)
                            } else {
                                openAudioFile(
                                    activity = requireActivity(),
                                    item = item,
                                    index = index
                                )
                            }
                        },
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
        sortByHeaderViewModel.orderChangeEvent.observe(
            viewLifecycleOwner, EventObserver { audioSectionViewModel.refreshWhenOrderChanged() }
        )

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
            if (!audioSectionViewModel.state.value.searchMode && list.isNotEmpty()) {
                callManager {
                    it.invalidateOptionsMenu()
                }
            }
        }
    }

    private fun showSortByPanel() {
        (requireActivity() as? ManagerActivity)?.showNewSortByPanel(Constants.ORDER_CLOUD)
    }

    private fun openAudioFile(
        activity: Activity,
        item: AudioUiEntity,
        index: Int,
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            val nodeHandle = item.id.longValue
            val fileType = item.fileTypeInfo.mimeType
            val intent = getIntent(item = item, index = index)

            activity.startActivity(
                audioSectionViewModel.isLocalFile(nodeHandle)?.let { localPath ->
                    File(localPath).let { mediaFile ->
                        runCatching {
                            FileProvider.getUriForFile(
                                activity,
                                Constants.AUTHORITY_STRING_FILE_PROVIDER,
                                mediaFile
                            )
                        }.onFailure {
                            Uri.fromFile(mediaFile)
                        }.map { mediaFileUri ->
                            intent.setDataAndType(mediaFileUri, fileType)
                            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }
                    }
                    intent
                } ?: audioSectionViewModel.updateIntent(
                    handle = nodeHandle,
                    fileType = fileType,
                    intent = intent
                )
            )
        }
    }

    private fun getIntent(
        item: AudioUiEntity,
        index: Int,
    ) = getMediaIntent(activity, item.name).apply {
        putExtra(INTENT_EXTRA_KEY_POSITION, index)
        putExtra(INTENT_EXTRA_KEY_HANDLE, item.id.longValue)
        putExtra(INTENT_EXTRA_KEY_FILE_NAME, item.name)
        val state = audioSectionViewModel.state.value
        if (state.searchMode) {
            putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, SEARCH_BY_ADAPTER)
            putExtra(
                INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH,
                state.allAudios.map { it.id.longValue }.toLongArray()
            )
        } else {
            putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, AUDIO_BROWSE_ADAPTER)
        }
        putExtra(
            INTENT_EXTRA_KEY_ORDER_GET_CHILDREN,
            sortByHeaderViewModel.cloudSortOrder.value
        )
        addFlags(FLAG_ACTIVITY_SINGLE_TOP)
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

    /**
     * Should show search menu
     *
     * @return true if should show search menu, false otherwise
     */
    override fun shouldShowSearchMenu(): Boolean = audioSectionViewModel.shouldShowSearchMenu()

    /**
     * Search ready
     */
    override fun searchReady() {
        audioSectionViewModel.searchReady()
    }

    /**
     * Search query
     *
     * @param query query string
     */
    override fun searchQuery(query: String) {
        audioSectionViewModel.searchQuery(query)
    }

    /**
     * Exit search
     */
    override fun exitSearch() {
        audioSectionViewModel.exitSearch()
    }

    suspend fun handleHideNodeClick() {
        var isPaid: Boolean
        var isHiddenNodesOnboarded: Boolean
        with(audioSectionViewModel.state.value) {
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
            audioSectionViewModel.hideOrUnhideNodes(
                nodeIds = audioSectionViewModel.getSelectedNodes().map { it.id },
                hide = true,
            )
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