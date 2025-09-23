package mega.privacy.android.app.presentation.shares.outgoing

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.sample
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
import mega.privacy.android.app.presentation.contact.authenticitycredendials.AuthenticityCredentialsActivity
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.mapper.GetOptionsForToolbarMapper
import mega.privacy.android.app.presentation.mapper.OptionsItemInfo
import mega.privacy.android.app.presentation.node.action.HandleNodeAction
import mega.privacy.android.app.presentation.photos.albums.add.AddToAlbumActivity
import mega.privacy.android.app.presentation.shares.SharesActionListener
import mega.privacy.android.app.presentation.shares.outgoing.ui.OutgoingSharesView
import mega.privacy.android.app.presentation.snackbar.LegacySnackBarWrapper
import mega.privacy.android.app.presentation.transfers.starttransfer.view.StartTransferComponent
import mega.privacy.android.app.sync.fileBackups.FileBackupManager
import mega.privacy.android.app.utils.CloudStorageOptionControlUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.wrapper.MegaNodeUtilWrapper
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.core.nodecomponents.model.NodeSourceTypeInt
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.shares.ShareNode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar
import mega.privacy.android.shared.resources.R as sharedR
import timber.log.Timber
import javax.inject.Inject

/**
 * A Fragment for Out going shares
 */
@AndroidEntryPoint
class OutgoingSharesComposeFragment : Fragment() {

    companion object {
        /**
         * Returns the instance of OutgoingSharesComposeFragment
         */
        @JvmStatic
        fun newInstance() = OutgoingSharesComposeFragment()
    }

    private var fileBackupManager: FileBackupManager? = null

    private var actionMode: ActionMode? = null

    /**
     * Interface that notifies the attached Activity to execute specific functions
     */
    private var outgoingSharesActionListener: SharesActionListener? = null

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
     * Mapper to get file type icon
     */
    @Inject
    lateinit var fileTypeIconMapper: FileTypeIconMapper

    @Inject
    lateinit var getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase

    @Inject
    lateinit var megaNodeUtilWrapper: MegaNodeUtilWrapper

    private val viewModel: OutgoingSharesComposeViewModel by activityViewModels()
    private val sortByHeaderViewModel: SortByHeaderViewModel by activityViewModels()

    /**
     * Toggle the elevation of the app bar
     */
    var toggleAppBarElevation: (Boolean) -> Unit = {}

    private val addToAlbumLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            ::handleAddToAlbumResult,
        )

    /**
     * onAttach
     */
    override fun onAttach(context: Context) {
        Timber.d("onAttach")
        super.onAttach(context)

        outgoingSharesActionListener = requireActivity() as? SharesActionListener
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

    /**
     * onCreateView
     */
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
                val uiState by viewModel.state.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }
                val coroutineScope = rememberCoroutineScope()
                var clickedFile: TypedFileNode? by remember {
                    mutableStateOf(null)
                }
                OriginalTheme(isDark = themeMode.isDarkMode()) {
                    OutgoingSharesView(
                        uiState = uiState,
                        emptyState = getEmptyFolderDrawable(uiState.isOutgoingSharesEmpty),
                        onItemClick = {
                            if (uiState.selectedNodeHandles.isEmpty()) {
                                when (it.node) {
                                    is TypedFileNode -> clickedFile = it.node

                                    is TypedFolderNode -> {
                                        viewModel.onFolderItemClicked(it.id.longValue)
                                    }

                                    else -> Timber.e("Unsupported click")
                                }
                            } else {
                                viewModel.onItemClicked(it)
                            }
                        },
                        onLongClick = {
                            val clicked = viewModel.onLongItemClicked(it)
                            if (clicked && actionMode == null) {
                                actionMode =
                                    (activity as? AppCompatActivity)?.startSupportActionMode(
                                        ActionBarCallBack()
                                    )
                            }
                        },
                        onMenuClick = {
                            if (uiState.isConnected) {
                                showOptionsMenuForItem(
                                    nodeUIItem = it,
                                    isContactVerificationOn = uiState.isContactVerificationOn
                                )
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
                        onSortOrderClick = { showSortByPanel() },
                        onChangeViewTypeClick = viewModel::onChangeViewTypeClicked,
                        onLinkClicked = context::launchUrl,
                        onVerifyContactDialogDismissed = viewModel::dismissVerifyContactDialog,
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
                LaunchedEffect(uiState.isInRootLevel) {
                    if (!uiState.isInRootLevel) {
                        toggleAppBarElevation(false)
                    }
                }
                LaunchedEffect(uiState.nodesList.isEmpty()) {
                    outgoingSharesActionListener?.updateSharesPageToolbarTitleAndFAB(
                        invalidateOptionsMenu = true
                    )
                }
                LaunchedEffect(uiState.optionsItemInfo) {
                    performItemOptionsClick(uiState.optionsItemInfo)
                }
                LaunchedEffect(uiState.totalSelectedFileNodes, uiState.totalSelectedFolderNodes) {
                    updateActionModeTitle(
                        fileCount = uiState.totalSelectedFileNodes,
                        folderCount = uiState.totalSelectedFolderNodes
                    )
                }
                clickedFile?.let {
                    HandleNodeAction(
                        typedFileNode = it,
                        nodeSourceType = NodeSourceTypeInt.OUTGOING_SHARES_ADAPTER,
                        sortOrder = uiState.sortOrder,
                        snackBarHostState = snackbarHostState,
                        onActionHandled = {
                            clickedFile = null
                        },
                        onDownloadEvent = viewModel::onDownloadFileTriggered,
                        coroutineScope = coroutineScope
                    )
                }
                ToolbarTitleUpdateEffect(uiState.updateToolbarTitleEvent) {
                    viewModel.consumeUpdateToolbarTitleEvent()
                }
                OutgoingSharesExitEffect(uiState.exitOutgoingSharesEvent) {
                    viewModel.consumeExitOutgoingSharesEvent()
                }
                EventEffect(
                    event = uiState.openAuthenticityCredentials,
                    onConsumed = viewModel::consumeOpenAuthenticityCredentials,
                    action = { email -> openAuthenticityCredentials(email) },
                )
            }
        }
    }

    /**
     * A Composable [EventEffect] to refresh the Toolbar Title
     *
     * @param event The State Event
     * @param onConsumeEvent Executes specific action if [event] has been consumed
     */
    @Composable
    private fun ToolbarTitleUpdateEffect(
        event: StateEvent,
        onConsumeEvent: () -> Unit,
    ) {
        EventEffect(
            event = event,
            onConsumed = onConsumeEvent,
            action = {
                outgoingSharesActionListener?.updateSharesPageToolbarTitleAndFAB(
                    invalidateOptionsMenu = true
                )
            },
        )
    }

    /**
     * A Composable [EventEffect] to exit the Outgoing Shares
     *
     * @param event The State Event
     * @param onConsumeEvent Executes specific action if [event] has been consumed
     */
    @Composable
    private fun OutgoingSharesExitEffect(
        event: StateEvent,
        onConsumeEvent: () -> Unit,
    ) {
        EventEffect(
            event = event,
            onConsumed = onConsumeEvent,
            action = { outgoingSharesActionListener?.exitSharesPage() },
        )
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
     * onViewCreated
     */
    @OptIn(FlowPreview::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.collectFlow(viewModel.state.map { it.isPendingRefresh }
            .sample(500L)) { isPendingRefresh ->
            if (isPendingRefresh) {
                viewModel.apply {
                    refreshNodes()
                    markHandledPendingRefresh()
                }
            }
        }

        viewLifecycleOwner.collectFlow(sortByHeaderViewModel.orderChangeState) {
            viewModel.onSortOrderChanged()
        }
    }

    /**
     * Get empty state for Outgoing Shares
     * @param isPageEmpty true when there's no outgoing shares
     */
    private fun getEmptyFolderDrawable(isPageEmpty: Boolean): Pair<Int, Int> {
        return if (isPageEmpty) {
            Pair(iconPackR.drawable.ic_folder_arrow_down_glass, R.string.context_empty_outgoing)
        } else {
            Pair(iconPackR.drawable.ic_empty_folder_glass, R.string.file_browser_empty_folder_new)
        }
    }

    /**
     * Shows Options menu for item clicked
     */
    private fun showOptionsMenuForItem(
        nodeUIItem: NodeUIItem<ShareNode>,
        isContactVerificationOn: Boolean,
    ) {
        // shareData.count = 0 means it's a distinct unverified share
        val shareData = if (isContactVerificationOn && nodeUIItem.node.shareData?.count == 0) {
            nodeUIItem.node.shareData
        } else {
            null
        }
        (requireActivity() as ManagerActivity).showNodeOptionsPanel(
            nodeId = nodeUIItem.id,
            mode = NodeOptionsBottomSheetDialogFragment.DEFAULT_MODE,
            shareData = shareData,
            hideHiddenActions = true,
        )
    }

    /**
     * Shows the Sort by panel.
     */
    private fun showSortByPanel() {
        val orderType = when (viewModel.getCurrentNodeHandle()) {
            -1L -> Constants.ORDER_OUTGOING_SHARES
            else -> Constants.ORDER_CLOUD
        }
        (requireActivity() as ManagerActivity).showNewSortByPanel(orderType)
    }

    private inner class ActionBarCallBack : ActionMode.Callback {
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
                viewModel.state.value.selectedNodes.takeUnless { it.isEmpty() }
                    ?: return false
            menu.findItem(R.id.cab_menu_share_link).title =
                resources.getQuantityString(sharedR.plurals.label_share_links, selected.size)
            lifecycleScope.launch {
                val control = getOptionsForToolbarMapper(
                    selectedNodeHandleList = viewModel.state.value.selectedNodeHandles,
                    totalNodes = viewModel.state.value.nodesList.size
                )
                // Slight customization for outgoing shares page
                control.move().isVisible = false
                control.hide().isVisible = false
                control.unhide().isVisible = false
                val areAllNotTakenDown = selected.any { it.isTakenDown.not() }
                if (areAllNotTakenDown) {
                    if (viewModel.state.value.isInRootLevel) {
                        control.removeShare().setVisible(true).showAsAction =
                            MenuItem.SHOW_AS_ACTION_ALWAYS
                    }
                    control.shareOut().setVisible(true).showAsAction =
                        MenuItem.SHOW_AS_ACTION_ALWAYS

                    if (selected.size > 1) {
                        control.manageLink().setVisible(false)
                        control.removeLink().setVisible(false)
                        control.link.setVisible(false)
                    }
                    control.copy().isVisible = true
                    if (control.alwaysActionCount() < CloudStorageOptionControlUtil.MAX_ACTION_COUNT) {
                        control.copy().showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
                    } else {
                        control.copy().showAsAction = MenuItem.SHOW_AS_ACTION_NEVER
                    }
                }

                val mediaNodes = selected
                    .filter {
                        val type = (it as? FileNode)?.type
                        type is ImageFileTypeInfo || type is VideoFileTypeInfo
                    }
                if (mediaNodes.size == selected.size) {
                    if (mediaNodes.all { (it as? FileNode)?.type is VideoFileTypeInfo }) {
                        control.addToAlbum().isVisible = false
                        control.addTo().isVisible = true
                    } else {
                        control.addToAlbum().isVisible = true
                        control.addTo().isVisible = false
                    }
                } else {
                    control.addToAlbum().isVisible = false
                    control.addTo().isVisible = false
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
            viewModel.clearAllNodes()
            (activity as? ManagerActivity)?.let {
                it.showFabButton()
                it.showHideBottomNavigationView(false)
                actionMode = null
            }
        }
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

                // Favorite actions not supported in outgoing shares
                OptionItems.ADD_TO_FAVOURITES_CLICKED, OptionItems.REMOVE_FROM_FAVOURITES_CLICKED -> {}

                // Label actions not supported in outgoing shares
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

                OptionItems.ADD_TO_ALBUM -> {
                    val intent = Intent(requireContext(), AddToAlbumActivity::class.java).apply {
                        val ids = viewModel.state.value.selectedNodeHandles.toTypedArray()
                        putExtra("ids", ids)
                        putExtra("type", 0)
                    }
                    addToAlbumLauncher.launch(intent)
                    disableSelectMode()
                }

                OptionItems.ADD_TO -> {
                    val intent = Intent(requireContext(), AddToAlbumActivity::class.java).apply {
                        val ids = viewModel.state.value.selectedNodeHandles.toTypedArray()
                        putExtra("ids", ids)
                        putExtra("type", 1)
                    }
                    addToAlbumLauncher.launch(intent)
                    disableSelectMode()
                }
            }
        }
    }

    /**
     * Disable select mode in OutgoingSharesComposeFragment
     */
    fun disableSelectMode() {
        viewModel.clearAllNodes()
        actionMode?.finish()
    }

    /**
     * Open authenticityCredentials screen to verify user
     * @param email : Email of the user
     */
    private fun openAuthenticityCredentials(email: String?) {
        Intent(
            requireActivity(),
            AuthenticityCredentialsActivity::class.java
        ).apply {
            putExtra(Constants.EMAIL, email)
            requireActivity().startActivity(this)
        }
    }

    private fun handleAddToAlbumResult(result: ActivityResult) {
        if (result.resultCode != RESULT_OK) return
        val message = result.data?.getStringExtra("message") ?: return

        Util.showSnackbar(requireActivity(), message)
    }
}