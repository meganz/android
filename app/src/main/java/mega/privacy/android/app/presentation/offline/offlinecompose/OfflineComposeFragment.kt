package mega.privacy.android.app.presentation.offline.offlinecompose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import de.palm.composestateevents.EventEffect
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.homepage.main.HomepageFragment
import mega.privacy.android.app.fragments.homepage.main.HomepageFragmentDirections
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.offline.action.HandleOfflineNodeActions
import mega.privacy.android.app.presentation.offline.action.OfflineNodeActionsViewModel
import mega.privacy.android.app.presentation.offline.confirmremovedialog.ConfirmRemoveFromOfflineDialogFragment
import mega.privacy.android.app.presentation.offline.optionbottomsheet.OfflineOptionsBottomSheetDialogFragment
import mega.privacy.android.app.presentation.offline.view.OfflineFeatureScreen
import mega.privacy.android.app.presentation.snackbar.LegacySnackBarWrapper
import mega.privacy.android.app.presentation.transfers.starttransfer.StartDownloadViewModel
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.callManager
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import timber.log.Timber
import javax.inject.Inject

/**
 * OfflineFragment with Compose
 */
@AndroidEntryPoint
class OfflineComposeFragment : Fragment(), ActionMode.Callback {

    /**
     * getThemeMode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    /**
     * fileTypeIconMapper [FileTypeIconMapper]
     */
    @Inject
    lateinit var fileTypeIconMapper: FileTypeIconMapper

    private val viewModel: OfflineComposeViewModel by viewModels()
    private val offlineNodeActionsViewModel: OfflineNodeActionsViewModel by activityViewModels()
    private val startDownloadViewModel: StartDownloadViewModel by activityViewModels()
    private val args: OfflineComposeFragmentArgs by navArgs()
    private var actionMode: ActionMode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments == null) {
            arguments =
                HomepageFragmentDirections.actionHomepageToFullscreenOfflineCompose().arguments
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val backgroundColor = Color(
            ColorUtils.getColorForElevation(
                requireContext(),
                Util.dp2px(HomepageFragment.BOTTOM_SHEET_ELEVATION).toFloat()
            )
        )
        return ComposeView(requireContext()).apply {
            setContent {
                val themeMode by getThemeMode()
                    .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                val isDarkMode = themeMode.isDarkMode()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val defaultTitle = stringResource(id = R.string.section_saved_for_offline_new)
                val snackbarHostState = remember { SnackbarHostState() }
                val coroutineScope = rememberCoroutineScope()

                OriginalTempTheme(isDark = isDarkMode) {
                    OfflineFeatureScreen(
                        uiState = uiState,
                        backgroundColor = if (args.rootFolderOnly) backgroundColor else MaterialTheme.colors.background,
                        fileTypeIconMapper = fileTypeIconMapper,
                        rootFolderOnly = args.rootFolderOnly,
                        onCloseWarningClick = viewModel::dismissOfflineWarning,
                        onOfflineItemClicked = {
                            viewModel.onItemClicked(it, args.rootFolderOnly)
                        },
                        onItemLongClicked = {
                            if (args.rootFolderOnly) {
                                viewModel.onItemClicked(it, args.rootFolderOnly)
                            } else {
                                viewModel.onLongItemClicked(it)
                                if (actionMode == null) {
                                    actionMode = (requireActivity() as AppCompatActivity)
                                        .startSupportActionMode(this@OfflineComposeFragment)
                                }
                            }
                        },
                        onOptionClicked = {
                            showOptionPanelBottomSheet(it.offlineNode.handle.toLong())
                        }
                    )
                    LaunchedEffect(uiState.title, uiState.offlineNodes) {
                        (requireActivity() as? ManagerActivity)?.setToolbarTitleFromFullscreenOfflineFragment(
                            title = uiState.title.ifEmpty { defaultTitle },
                            firstNavigationLevel = false,
                            showSearch = true
                        )
                    }
                    LaunchedEffect(uiState.selectedNodeHandles.size) {
                        updateActionModeTitle(
                            selectedItemsCount = uiState.selectedNodeHandles.size
                        )
                    }
                    HandleOfflineNodeActions(
                        viewModel = offlineNodeActionsViewModel,
                        snackBarHostState = snackbarHostState,
                        coroutineScope = coroutineScope
                    )
                    EventEffect(
                        event = uiState.openFolderInPageEvent,
                        onConsumed = viewModel::onOpenFolderInPageEventConsumed
                    ) {
                        findNavController().navigate(
                            HomepageFragmentDirections.actionHomepageToFullscreenOfflineCompose(
                                rootFolderOnly = false,
                                parentId = it.id,
                                title = it.name
                            )
                        )
                    }
                    EventEffect(
                        event = uiState.openOfflineNodeEvent,
                        onConsumed = viewModel::onOpenOfflineNodeEventConsumed
                    ) {
                        offlineNodeActionsViewModel.handleOpenOfflineFile(it)
                    }
                    EventEffect(
                        event = uiState.closeSearchViewEvent,
                        onConsumed = viewModel::onCloseSearchViewEventConsumed
                    ) {
                        callManager {
                            it.closeSearchView()
                        }
                    }
                }

                LegacySnackBarWrapper(snackbarHostState = snackbarHostState, activity)
            }
        }
    }

    /**
     * Checks if the fragment is in search mode
     */
    fun isInSearchMode() = with(viewModel.uiState.value) {
        !searchQuery.isNullOrBlank() && offlineNodes.isNotEmpty()
    }

    /**
     * Sets the Search Query. This function is only being used by [ManagerActivity]
     *
     * @param query The text to find items. It may be nullable
     */
    fun setSearchQuery(query: String?) {
        viewModel.setSearchQuery(query)
    }

    private fun showOptionPanelBottomSheet(nodeHandle: Long) {
        val tag = OfflineOptionsBottomSheetDialogFragment::class.java.simpleName
        if (childFragmentManager.findFragmentByTag(tag) != null) return
        OfflineOptionsBottomSheetDialogFragment.newInstance(nodeHandle)
            .show(
                childFragmentManager,
                OfflineOptionsBottomSheetDialogFragment::class.java.simpleName
            )
    }

    /**
     * On view created
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        callManager {
            if (args.rootFolderOnly) {
                it.pagerOfflineComposeFragmentOpened(this)
            } else {
                it.fullscreenOfflineFragmentComposeOpened(this)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        callManager {
            if (args.rootFolderOnly) {
                it.pagerOfflineComposeFragmentClosed(this)
            } else {
                it.fullscreenOfflineFragmentComposeClosed(this)
            }
        }
    }

    /**
     * On action mode finished
     */
    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        Timber.d("ActionBarCallBack::onPrepareActionMode")

        menu?.findItem(R.id.cab_menu_select_all)?.isVisible =
            viewModel.uiState.value.selectedNodeHandles.size < viewModel.uiState.value.offlineNodes.size

        return true
    }

    /**
     * On create action mode
     */
    override fun onCreateActionMode(mode: ActionMode, menu: Menu?): Boolean {
        Timber.d("ActionBarCallBack::onCreateActionMode")
        val inflater = mode.menuInflater
        inflater.inflate(R.menu.offline_browser_action, menu)
        return true
    }

    /**
     * On destroy action mode
     */
    override fun onDestroyActionMode(mode: ActionMode?) {
        viewModel.clearSelection()
        actionMode = null
    }

    /**
     * On action item clicked
     */
    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.cab_menu_download -> {
                val nodes = viewModel.uiState.value.selectedNodeHandles
                if (nodes.isNotEmpty()) {
                    startDownloadViewModel.onCopyOfflineNodeClicked(
                        nodes.map { NodeId(it) }
                    )
                }
                viewModel.clearSelection()
            }

            R.id.cab_menu_share_out -> {
                offlineNodeActionsViewModel.handleShareOfflineNodes(
                    nodes = viewModel.uiState.value.selectedOfflineNodes,
                    isOnline = viewModel.uiState.value.isOnline
                )
                viewModel.clearSelection()
            }

            R.id.cab_menu_delete -> {
                showConfirmRemoveFromOfflineDialog(viewModel.uiState.value.selectedNodeHandles)
                viewModel.clearSelection()
            }

            R.id.cab_menu_select_all -> {
                viewModel.selectAll()
            }

            R.id.cab_menu_clear_selection -> {
                viewModel.clearSelection()
                actionMode?.finish()
            }
        }

        return false
    }

    /**
     * On back pressed or clicked
     */
    fun onBackPressed(): Int? = viewModel.onBackClicked()

    private fun showConfirmRemoveFromOfflineDialog(handles: List<Long>) {
        ConfirmRemoveFromOfflineDialogFragment.newInstance(handles)
            .show(
                requireActivity().supportFragmentManager,
                ConfirmRemoveFromOfflineDialogFragment::class.java.simpleName
            )
    }

    private fun updateActionModeTitle(selectedItemsCount: Int) {
        actionMode?.let {
            actionMode?.title = when {
                (selectedItemsCount == 0) -> {
                    actionMode?.finish()
                    0.toString()
                }

                else -> "$selectedItemsCount"
            }

            runCatching {
                actionMode?.invalidate()
            }.onFailure {
                Timber.e(it, "Invalidate error")
            }
        } ?: run {
            return
        }
    }
}