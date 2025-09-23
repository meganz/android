package mega.privacy.android.app.presentation.shares.links

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import de.palm.composestateevents.EventEffect
import de.palm.composestateevents.StateEvent
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.extensions.launchUrl
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.interfaces.ActionBackupListener
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.main.dialog.removelink.RemovePublicLinkDialogFragment
import mega.privacy.android.app.main.dialog.rubbishbin.ConfirmMoveToRubbishBinDialogFragment
import mega.privacy.android.app.main.dialog.shares.RemoveAllSharingContactDialogFragment
import mega.privacy.android.app.presentation.bottomsheet.NodeOptionsBottomSheetDialogFragment
import mega.privacy.android.app.presentation.clouddrive.OptionItems
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.manager.model.SharesTab
import mega.privacy.android.app.presentation.manager.model.Tab
import mega.privacy.android.app.presentation.mapper.GetOptionsForToolbarMapper
import mega.privacy.android.app.presentation.mapper.OptionsItemInfo
import mega.privacy.android.app.presentation.node.action.HandleNodeAction
import mega.privacy.android.app.presentation.shares.SharesActionListener
import mega.privacy.android.app.presentation.shares.links.view.LinksView
import mega.privacy.android.app.presentation.snackbar.LegacySnackBarWrapper
import mega.privacy.android.app.presentation.transfers.starttransfer.view.StartTransferComponent
import mega.privacy.android.app.sync.fileBackups.FileBackupManager
import mega.privacy.android.app.utils.CloudStorageOptionControlUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.wrapper.MegaNodeUtilWrapper
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.core.nodecomponents.model.NodeSourceTypeInt
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFile
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkNode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar
import mega.privacy.android.shared.resources.R as sharedR
import timber.log.Timber
import javax.inject.Inject

/**
 * OfflineFileInfoFragment with Compose
 */
@AndroidEntryPoint
class LinksComposeFragment : Fragment() {

    private val viewModel: LinksViewModel by activityViewModels()
    private val sortByHeaderViewModel: SortByHeaderViewModel by activityViewModels()

    /**
     * Mapper to get options for Action Bar
     */
    @Inject
    lateinit var getOptionsForToolbarMapper: GetOptionsForToolbarMapper

    /**
     * Mapper to get current theme mode
     */
    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    /**
     * Mapper to get file type icon
     */
    @Inject
    lateinit var fileTypeIconMapper: FileTypeIconMapper

    @Inject
    lateinit var getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase

    @Inject
    lateinit var megaNodeUtilWrapper: MegaNodeUtilWrapper

    /**
     * Interface that notifies the attached Activity to execute specific functions
     */
    private var linksActionListener: SharesActionListener? = null
    private var fileBackupManager: FileBackupManager? = null
    private var actionMode: ActionMode? = null

    /**
     * Toggle the elevation of the app bar
     */
    var toggleAppBarElevation: (Boolean) -> Unit = {}

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val themeMode by monitorThemeModeUseCase()
                    .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                val isDarkMode = themeMode.isDarkMode()
                val uiState by viewModel.state.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }
                val coroutineScope = rememberCoroutineScope()
                var currentFileNode: TypedFileNode? by remember {
                    mutableStateOf(null)
                }

                OriginalTheme(isDark = isDarkMode) {
                    LinksView(
                        uiState = uiState,
                        emptyState = getEmptyFolderDrawable(uiState.isLinksEmpty),
                        onItemClick = {
                            if (uiState.selectedNodeHandles.isEmpty()) {
                                when (val item = it.node) {
                                    is PublicLinkFile -> {
                                        currentFileNode = item.node
                                    }

                                    is TypedFileNode -> {
                                        currentFileNode = item
                                    }

                                    is TypedFolderNode -> {
                                        viewModel.openFolderByHandle(item.id.longValue)
                                    }

                                    else -> Timber.e("Unsupported click")
                                }

                            } else {
                                viewModel.onItemClicked(it)
                            }
                        },
                        onLongClick = {
                            viewModel.onLongItemClicked(it)
                            if (actionMode == null) {
                                actionMode =
                                    (activity as? AppCompatActivity)?.startSupportActionMode(
                                        ActionBarCallBack(SharesTab.LINKS_TAB)
                                    )
                            }
                        },
                        onMenuClick = {
                            if (uiState.isConnected) {
                                showOptionsMenuForItem(it)
                            } else {
                                coroutineScope.launch {
                                    snackbarHostState.showAutoDurationSnackbar(
                                        message = getString(R.string.error_server_connection_problem),
                                    )
                                }
                            }
                        },
                        sortOrder = getString(
                            SortByHeaderViewModel.orderNameMap[uiState.sortOrder]
                                ?: R.string.sortby_name
                        ),
                        onLinkClick = context::launchUrl,
                        onSortOrderClick = ::showSortByPanel,
                        onToggleAppBarElevation = toggleAppBarElevation,
                        fileTypeIconMapper = fileTypeIconMapper,
                    )
                    LegacySnackBarWrapper(snackbarHostState = snackbarHostState, activity)
                    StartTransferComponent(
                        uiState.downloadEvent,
                        {
                            viewModel.consumeDownloadEvent()
                            disableSelectMode()
                        },
                        snackBarHostState = snackbarHostState,
                    )
                }

                performItemOptionsClick(uiState.optionsItemInfo)
                updateActionModeTitle(
                    fileCount = uiState.selectedFileNodes,
                    folderCount = uiState.selectedFolderNodes
                )
                currentFileNode?.let {
                    HandleNodeAction(
                        typedFileNode = it,
                        nodeSourceType = NodeSourceTypeInt.LINKS_ADAPTER,
                        sortOrder = uiState.sortOrder,
                        snackBarHostState = snackbarHostState,
                        onActionHandled = {
                            currentFileNode = null
                        },
                        onDownloadEvent = viewModel::onDownloadFileTriggered,
                        coroutineScope = coroutineScope
                    )
                } ?: run {
                    linksActionListener?.updateSharesPageToolbarTitleAndFAB(
                        invalidateOptionsMenu = true
                    )
                }
                UpdateToolbarTitle(uiState.updateToolbarTitleEvent) {
                    viewModel.consumeUpdateToolbarTitleEvent()
                }
                ExitLinkPage(uiState.exitLinksPageEvent) {
                    viewModel.consumeExitLinksPageEvent()
                }
            }
        }
    }

    /**
     * Get empty state for FileBrowser
     * @param isLinksEmpty
     */
    private fun getEmptyFolderDrawable(isLinksEmpty: Boolean): Pair<Int, Int> {
        return if (isLinksEmpty) {
            Pair(iconPackR.drawable.ic_link_glass, R.string.context_empty_links)
        } else {
            Pair(iconPackR.drawable.ic_empty_folder_glass, R.string.file_browser_empty_folder_new)
        }
    }

    /**
     * A Composable [EventEffect] to refresh the Toolbar Title
     *
     * @param event The State Event
     * @param onConsumeEvent Executes specific action if [event] has been consumed
     */
    @Composable
    private fun UpdateToolbarTitle(
        event: StateEvent,
        onConsumeEvent: () -> Unit,
    ) {
        EventEffect(
            event = event,
            onConsumed = onConsumeEvent,
            action = {
                linksActionListener?.updateSharesPageToolbarTitleAndFAB(invalidateOptionsMenu = true)
            },
        )
    }

    /**
     * A Composable [EventEffect] to exit the Links page
     *
     * @param event The State Event
     * @param onConsumeEvent Executes specific action if [event] has been consumed
     */
    @Composable
    private fun ExitLinkPage(
        event: StateEvent,
        onConsumeEvent: () -> Unit,
    ) {
        EventEffect(
            event = event,
            onConsumed = onConsumeEvent,
            action = { linksActionListener?.exitSharesPage() },
        )
    }

    /**
     * Shows Options menu for item clicked
     */
    private fun showOptionsMenuForItem(nodeUIItem: NodeUIItem<PublicLinkNode>) {
        (requireActivity() as ManagerActivity).showNodeOptionsPanel(
            nodeId = nodeUIItem.id,
            mode = NodeOptionsBottomSheetDialogFragment.DEFAULT_MODE,
            hideHiddenActions = true,
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sortByHeaderViewModel.refreshData(isUpdatedOrderChangeState = true)
        viewLifecycleOwner.collectFlow(sortByHeaderViewModel.orderChangeState) {
            viewModel.refreshLinkNodes(false)
        }

        viewLifecycleOwner.collectFlow(
            viewModel.state
                .map { it.nodesList.isEmpty() }
                .distinctUntilChanged()) {
            linksActionListener?.updateSharesPageToolbarTitleAndFAB(invalidateOptionsMenu = true)
        }

        viewLifecycleOwner.collectFlow(
            viewModel.state
                .map { it.parentNode != null }
                .distinctUntilChanged()) {
            toggleAppBarElevation(false)
        }
    }

    /**
     * Updates title of action mode
     */
    private fun updateActionModeTitle(fileCount: Int, folderCount: Int) {
        actionMode?.let {
            actionMode?.title = when {
                (fileCount == 0 && folderCount == 0) -> {
                    actionMode?.finish()
                    0.toString()
                }

                fileCount == 0 -> folderCount.toString()
                folderCount == 0 -> fileCount.toString()
                else -> (fileCount + folderCount).toString()
            }

            runCatching {
                actionMode?.invalidate()
            }.onFailure {
                Timber.e(it, "Invalidate error")
            }
        }
    }

    /**
     * Shows the Sort by panel.
     */
    private fun showSortByPanel() {
        (requireActivity() as ManagerActivity).showNewSortByPanel(Constants.ORDER_CLOUD)
    }

    /**
     *
     */
    override fun onAttach(context: Context) {
        super.onAttach(context)
        linksActionListener = requireActivity() as SharesActionListener
        fileBackupManager = FileBackupManager(
            activity = requireActivity(),
            actionBackupListener = object : ActionBackupListener {
                override fun actionBackupResult(
                    actionType: Int,
                    operationType: Int,
                    result: MoveRequestResult?,
                    handle: Long,
                ) {
                    Timber.d("Nothing to do for actionType = $actionType")
                }
            },
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            megaNodeUtilWrapper = megaNodeUtilWrapper,
        )
    }

    private fun performItemOptionsClick(optionsItemInfo: OptionsItemInfo?) {
        optionsItemInfo?.let {
            when (it.optionClickedType) {
                OptionItems.DOWNLOAD_CLICKED -> {
                    (requireActivity() as ManagerActivity).saveNodesToDevice(
                        nodes = it.selectedMegaNode,
                        highPriority = false,
                        isFolderLink = false,
                        fromChat = false,
                        withStartMessage = false,
                    )
                    disableSelectMode()
                }

                OptionItems.RENAME_CLICKED -> {
                    (requireActivity() as ManagerActivity).showRenameDialog(it.selectedMegaNode[0])
                    disableSelectMode()
                }

                OptionItems.SHARE_FOLDER_CLICKED -> {
                    it.selectedNode.filterIsInstance<FolderNode>()
                        .map { folderNode -> folderNode.id.longValue }
                        .let { handles ->
                            val nC = NodeController(requireActivity())
                            fileBackupManager?.let { backupManager ->
                                val handleList = ArrayList(handles)
                                if (!backupManager.hasBackupsNodes(
                                        nodeController = nC,
                                        handleList = handleList,
                                        actionBackupNodeCallback = backupManager.actionBackupNodeCallback,
                                    )
                                ) {
                                    nC.selectContactToShareFolders(handleList)
                                }
                            }
                        }
                    disableSelectMode()
                }

                OptionItems.SHARE_OUT_CLICKED -> {
                    MegaNodeUtil.shareNodes(requireContext(), it.selectedMegaNode)
                    disableSelectMode()
                }

                OptionItems.SHARE_EDIT_LINK_CLICKED -> {
                    (requireActivity() as ManagerActivity).showGetLinkActivity(it.selectedMegaNode)
                    disableSelectMode()
                }

                OptionItems.REMOVE_LINK_CLICKED -> {
                    RemovePublicLinkDialogFragment.newInstance(it.selectedNode.map { node -> node.id.longValue })
                        .show(childFragmentManager, RemovePublicLinkDialogFragment.TAG)
                    disableSelectMode()
                }

                OptionItems.SEND_TO_CHAT_CLICKED -> {
                    (requireActivity() as ManagerActivity).attachNodesToChats(it.selectedMegaNode)
                    disableSelectMode()
                }

                OptionItems.MOVE_TO_RUBBISH_CLICKED -> {
                    viewModel.state.value.selectedNodeHandles.takeIf { handles -> handles.isNotEmpty() }
                        ?.let { handles ->
                            ConfirmMoveToRubbishBinDialogFragment.newInstance(handles)
                                .show(
                                    requireActivity().supportFragmentManager,
                                    ConfirmMoveToRubbishBinDialogFragment.TAG
                                )
                            disableSelectMode()
                        }
                }

                OptionItems.REMOVE_SHARE_CLICKED -> {
                    RemoveAllSharingContactDialogFragment.newInstance(it.selectedNode.map { node -> node.id.longValue })
                        .show(childFragmentManager, RemoveAllSharingContactDialogFragment.TAG)
                    disableSelectMode()
                }

                OptionItems.SELECT_ALL_CLICKED -> {
                    viewModel.selectAllNodes()
                }

                OptionItems.CLEAR_ALL_CLICKED -> {
                    disableSelectMode()
                }

                OptionItems.HIDE_CLICKED, OptionItems.UNHIDE_CLICKED -> {}

                // Favorite actions not supported in links compose
                OptionItems.ADD_TO_FAVOURITES_CLICKED, OptionItems.REMOVE_FROM_FAVOURITES_CLICKED -> {}

                // Label actions not supported in links compose
                OptionItems.ADD_LABEL_CLICKED -> {}

                OptionItems.COPY_CLICKED -> {
                    val nC = NodeController(requireActivity())
                    nC.chooseLocationToCopyNodes(viewModel.state.value.selectedNodeHandles)
                    disableSelectMode()
                }

                OptionItems.MOVE_CLICKED -> {
                    val nC = NodeController(requireActivity())
                    nC.chooseLocationToMoveNodes(viewModel.state.value.selectedNodeHandles)
                    disableSelectMode()
                }

                OptionItems.DISPUTE_CLICKED -> {
                    context.launchUrl(Constants.DISPUTE_URL)
                    disableSelectMode()
                }

                // This option is only available in the Incoming Shares page
                OptionItems.LEAVE_SHARE_CLICKED -> Unit

                OptionItems.ADD_TO_ALBUM -> {}

                OptionItems.ADD_TO -> {}
            }
        }
    }

    /**
     * Disable select mode
     */
    fun disableSelectMode() {
        viewModel.clearAllNodesSelection()
        actionMode?.finish()
    }


    private inner class ActionBarCallBack(val currentTab: Tab) : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            val inflater = mode.menuInflater
            inflater.inflate(R.menu.cloud_storage_action, menu)
            (requireActivity() as ManagerActivity).let {
                it.hideFabButton()
                it.showHideBottomNavigationView(true)
            }
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            val selected =
                viewModel.state.value.selectedNodeHandles.takeUnless { it.isEmpty() }
                    ?: return false
            menu.findItem(R.id.cab_menu_share_link).title =
                resources.getQuantityString(sharedR.plurals.label_share_links, selected.size)
            lifecycleScope.launch {
                val control = getOptionsForToolbarMapper(
                    selectedNodeHandleList = viewModel.state.value.selectedNodeHandles,
                    totalNodes = viewModel.state.value.nodesList.size
                )

                // Slight customization for links page
                control.hide().isVisible = false
                control.unhide().isVisible = false
                control.move().isVisible = false
                control.removeShare().isVisible = false
                control.shareFolder().isVisible = false
                if (selected.size > 1) {
                    control.removeLink().setVisible(true).showAsAction =
                        MenuItem.SHOW_AS_ACTION_ALWAYS
                    control.link.apply {
                        isVisible = false
                        showAsAction = MenuItem.SHOW_AS_ACTION_NEVER
                    }
                }
                if (control.alwaysActionCount() < CloudStorageOptionControlUtil.MAX_ACTION_COUNT) {
                    control.copy().showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
                }
                CloudStorageOptionControlUtil.applyControl(menu, control)
            }
            return true
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            Timber.d("onActionItemClicked")
            viewModel.onOptionItemClicked(item)
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            viewModel.clearAllNodesSelection()
            (activity as? ManagerActivity)?.let {
                it.showFabButton()
                it.showHideBottomNavigationView(false)
                actionMode = null
            }
        }
    }
}

