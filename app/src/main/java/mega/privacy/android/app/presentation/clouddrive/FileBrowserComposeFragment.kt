package mega.privacy.android.app.presentation.clouddrive

import mega.privacy.android.shared.resources.R as sharedR
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.compose.material.rememberScaffoldState
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
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import de.palm.composestateevents.EventEffect
import de.palm.composestateevents.StateEvent
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.WebViewActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.interfaces.ActionBackupListener
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.main.dialog.removelink.RemovePublicLinkDialogFragment
import mega.privacy.android.app.main.dialog.rubbishbin.ConfirmMoveToRubbishBinDialogFragment
import mega.privacy.android.app.main.dialog.shares.RemoveAllSharingContactDialogFragment
import mega.privacy.android.app.presentation.advertisements.model.AdsSlotIDs.TAB_CLOUD_SLOT_ID
import mega.privacy.android.app.presentation.bottomsheet.NodeOptionsBottomSheetDialogFragment
import mega.privacy.android.app.presentation.clouddrive.ui.FileBrowserComposeView
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.hidenode.HiddenNodesOnboardingActivity
import mega.privacy.android.app.presentation.mapper.GetOptionsForToolbarMapper
import mega.privacy.android.app.presentation.mapper.OptionsItemInfo
import mega.privacy.android.app.presentation.node.NodeActionsViewModel
import mega.privacy.android.app.presentation.node.action.HandleNodeAction
import mega.privacy.android.app.presentation.transfers.starttransfer.view.StartTransferComponent
import mega.privacy.android.app.sync.fileBackups.FileBackupManager
import mega.privacy.android.app.utils.CloudStorageOptionControlUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar
import mega.privacy.mobile.analytics.event.CloudDriveHideNodeMenuItemEvent
import mega.privacy.mobile.analytics.event.CloudDriveScreenEvent
import timber.log.Timber
import javax.inject.Inject

/**
 * A Fragment for File Browser
 */
@AndroidEntryPoint
class FileBrowserComposeFragment : Fragment() {

    companion object {
        /**
         * Returns the instance of FileBrowserComposeFragment
         */
        @JvmStatic
        fun newInstance() = FileBrowserComposeFragment()
    }

    private var fileBackupManager: FileBackupManager? = null

    private var actionMode: ActionMode? = null

    /**
     * Interface that notifies the attached Activity to execute specific functions
     */
    private var fileBrowserActionListener: FileBrowserActionListener? = null

    /**
     * Application Theme Mode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    /**
     * File type icon mapper
     */
    @Inject
    lateinit var fileTypeIconMapper: FileTypeIconMapper

    /**
     * Mapper to get options for Action Bar
     */
    @Inject
    lateinit var getOptionsForToolbarMapper: GetOptionsForToolbarMapper

    private val nodeActionsViewModel: NodeActionsViewModel by viewModels()
    private val fileBrowserViewModel: FileBrowserViewModel by activityViewModels()
    private val sortByHeaderViewModel: SortByHeaderViewModel by activityViewModels()

    private var tempNodeIds: List<NodeId> = listOf()

    @Inject
    lateinit var getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase

    /**
     * onAttach
     */
    override fun onAttach(context: Context) {
        Timber.d("onAttach")
        super.onAttach(context)

        fileBrowserActionListener = requireActivity() as FileBrowserActionListener
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
            }
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
                val themeMode by getThemeMode()
                    .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                val uiState by fileBrowserViewModel.state.collectAsStateWithLifecycle()
                val nodeActionState by nodeActionsViewModel.state.collectAsStateWithLifecycle()
                val scaffoldState = rememberScaffoldState()
                val snackbarHostState = scaffoldState.snackbarHostState
                val coroutineScope = rememberCoroutineScope()
                var clickedFile: TypedFileNode? by remember {
                    mutableStateOf(null)
                }

                OriginalTempTheme(isDark = themeMode.isDarkMode()) {
                    MegaScaffold(
                        scaffoldState = scaffoldState,
                    ) {
                        FileBrowserComposeView(
                            uiState = uiState,
                            emptyState = getEmptyFolderDrawable(uiState.isFileBrowserEmpty),
                            onItemClick = {
                                if (uiState.selectedNodeHandles.isEmpty()) {
                                    when (it.node) {
                                        is TypedFileNode -> clickedFile = it.node

                                        is TypedFolderNode -> {
                                            fileBrowserViewModel.onFolderItemClicked(it.id.longValue)
                                        }

                                        else -> Timber.e("Unsupported click")
                                    }
                                } else {
                                    fileBrowserViewModel.onItemClicked(it)
                                }
                            },
                            onLongClick = {
                                fileBrowserViewModel.onLongItemClicked(it)
                                if (actionMode == null) {
                                    actionMode =
                                        (activity as? AppCompatActivity)?.startSupportActionMode(
                                            ActionBarCallBack()
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
                            onSortOrderClick = { showSortByPanel() },
                            onChangeViewTypeClick = fileBrowserViewModel::onChangeViewTypeClicked,
                            onLinkClicked = ::navigateToLink,
                            onDisputeTakeDownClicked = ::navigateToLink,
                            onDismissClicked = fileBrowserViewModel::onBannerDismissClicked,
                            onStorageFullWarningDismiss = {},
                            onUpgradeClicked = {
                                fileBrowserViewModel::onBannerDismissClicked
                                (activity as? ManagerActivity)?.navigateToUpgradeAccount()
                            },
                            onEnterMediaDiscoveryClick = {
                                disableSelectMode()
                                fileBrowserViewModel.setMediaDiscoveryVisibility(
                                    isMediaDiscoveryOpen = true,
                                    isMediaDiscoveryOpenedByIconClick = true,
                                )
                            },
                            fileTypeIconMapper = fileTypeIconMapper
                        )
                        StartTransferComponent(
                            uiState.downloadEvent,
                            {
                                fileBrowserViewModel.consumeDownloadEvent()
                                disableSelectMode()
                            },
                            snackBarHostState = snackbarHostState,
                        )
                        EventEffect(
                            event = nodeActionState.downloadEvent,
                            onConsumed = nodeActionsViewModel::markDownloadEventConsumed
                        ) {
                            fileBrowserViewModel.onDownloadFileTriggered(it)
                        }
                    }
                }
                performItemOptionsClick(uiState.optionsItemInfo)
                updateActionModeTitle(
                    fileCount = uiState.selectedFileNodes,
                    folderCount = uiState.selectedFolderNodes
                )
                HandleMediaDiscoveryVisibility(
                    isMediaDiscoveryOpen = uiState.isMediaDiscoveryOpen,
                    isMediaDiscoveryOpenedByIconClick = uiState.isMediaDiscoveryOpenedByIconClick,
                    mediaHandle = uiState.fileBrowserHandle,
                    errorMessage = uiState.errorMessage
                )
                UpdateToolbarTitle(uiState.updateToolbarTitleEvent) {
                    fileBrowserViewModel.consumeUpdateToolbarTitleEvent()
                }
                ExitFileBrowser(uiState.exitFileBrowserEvent) {
                    fileBrowserViewModel.consumeExitFileBrowserEvent()
                }
                clickedFile?.let {
                    HandleNodeAction(
                        typedFileNode = it,
                        nodeSourceType = Constants.FILE_BROWSER_ADAPTER,
                        sortOrder = uiState.sortOrder,
                        snackBarHostState = snackbarHostState,
                        onActionHandled = {
                            clickedFile = null
                        },
                        nodeActionsViewModel = nodeActionsViewModel,
                        coroutineScope = coroutineScope
                    )
                }
            }
        }
    }

    /**
     * onResume
     */
    override fun onResume() {
        super.onResume()
        Analytics.tracker.trackEvent(CloudDriveScreenEvent)
        Firebase.crashlytics.log("Screen: ${CloudDriveScreenEvent.eventName}")
    }

    /**
     * A Composable that checks if Media Discovery should be shown or not
     *
     * @param isMediaDiscoveryOpen If true, this indicates that Media Discovery is open
     * @param isMediaDiscoveryOpenedByIconClick true if Media Discovery was accessed by clicking the
     * Media Discovery Icon
     * @param mediaHandle The Handle used to display content in Media Discovery
     * @param errorMessage The [StringRes] of the error message to display
     */
    @Composable
    private fun HandleMediaDiscoveryVisibility(
        isMediaDiscoveryOpen: Boolean,
        isMediaDiscoveryOpenedByIconClick: Boolean,
        mediaHandle: Long,
        @StringRes errorMessage: Int?,
    ) {
        LaunchedEffect(
            isMediaDiscoveryOpen,
            isMediaDiscoveryOpenedByIconClick,
            mediaHandle,
            errorMessage
        ) {
            if (isMediaDiscoveryOpen) {
                fileBrowserActionListener?.showMediaDiscoveryFromCloudDrive(
                    mediaHandle = mediaHandle,
                    isAccessedByIconClick = isMediaDiscoveryOpenedByIconClick,
                    replaceFragment = fileBrowserViewModel.state().hasNoOpenedFolders,
                    errorMessage = errorMessage,
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
    private fun UpdateToolbarTitle(
        event: StateEvent,
        onConsumeEvent: () -> Unit,
    ) {
        EventEffect(
            event = event,
            onConsumed = onConsumeEvent,
            action = {
                fileBrowserActionListener?.updateCloudDriveToolbarTitle(invalidateOptionsMenu = true)
            },
        )
    }

    /**
     * A Composable [EventEffect] to exit the Cloud Drive
     *
     * @param event The State Event
     * @param onConsumeEvent Executes specific action if [event] has been consumed
     */
    @Composable
    private fun ExitFileBrowser(
        event: StateEvent,
        onConsumeEvent: () -> Unit,
    ) {
        EventEffect(
            event = event,
            onConsumed = onConsumeEvent,
            action = { fileBrowserActionListener?.exitCloudDrive() },
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
        sortByHeaderViewModel.refreshData(isUpdatedOrderChangeState = true)
        viewLifecycleOwner.collectFlow(fileBrowserViewModel.state.map { it.isPendingRefresh }
            .sample(500L)) { isPendingRefresh ->
            if (isPendingRefresh) {
                fileBrowserViewModel.apply {
                    refreshNodes()
                    markHandledPendingRefresh()
                }
            }
        }

        viewLifecycleOwner.collectFlow(fileBrowserViewModel.state.map { it.nodesList.isEmpty() }
            .distinctUntilChanged()) {
            fileBrowserActionListener?.updateCloudDriveToolbarTitle(invalidateOptionsMenu = true)
        }

        viewLifecycleOwner.collectFlow(sortByHeaderViewModel.orderChangeState) {
            fileBrowserViewModel.onCloudDriveSortOrderChanged()
        }
    }

    /**
     * Get empty state for FileBrowser
     * @param isCloudDriveEmpty
     */
    private fun getEmptyFolderDrawable(isCloudDriveEmpty: Boolean): Pair<Int, Int> {
        return if (isCloudDriveEmpty) {
            Pair(
                if (requireActivity().resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    R.drawable.ic_empty_cloud_drive
                } else {
                    R.drawable.ic_empty_cloud_drive
                }, R.string.context_empty_cloud_drive
            )
        } else {
            Pair(
                if (requireActivity().resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    R.drawable.ic_zero_landscape_empty_folder
                } else {
                    R.drawable.ic_zero_portrait_empty_folder
                }, R.string.file_browser_empty_folder_new
            )
        }
    }

    /**
     * Shows Options menu for item clicked
     */
    private fun showOptionsMenuForItem(nodeUIItem: NodeUIItem<TypedNode>) {
        (requireActivity() as ManagerActivity).showNodeOptionsPanel(
            nodeId = nodeUIItem.id,
            mode = NodeOptionsBottomSheetDialogFragment.CLOUD_DRIVE_MODE
        )
    }

    /**
     * Shows the Sort by panel.
     */
    private fun showSortByPanel() {
        (requireActivity() as ManagerActivity).showNewSortByPanel(Constants.ORDER_CLOUD)
    }

    private inner class ActionBarCallBack : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            val inflater = mode.menuInflater
            inflater.inflate(R.menu.cloud_storage_action, menu)
            (requireActivity() as ManagerActivity).let {
                it.hideFabButton()
                it.showHideBottomNavigationView(true)
                it.hideAdsView()
            }
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            val selected =
                fileBrowserViewModel.state.value.selectedNodeHandles.takeUnless { it.isEmpty() }
                    ?: return false
            val nodeList = fileBrowserViewModel.state.value.nodesList
            menu.findItem(R.id.cab_menu_share_link).title =
                resources.getQuantityString(sharedR.plurals.label_share_links, selected.size)
            lifecycleScope.launch {
                runCatching {
                    val control = getOptionsForToolbarMapper(
                        selectedNodeHandleList = fileBrowserViewModel.state.value.selectedNodeHandles,
                        totalNodes = fileBrowserViewModel.state.value.nodesList.size
                    )
                    CloudStorageOptionControlUtil.applyControl(menu, control)

                    handleHiddeNodes(selected, nodeList, menu)
                }.onFailure {
                    Timber.e(it)
                }
            }
            return true
        }

        private suspend fun handleHiddeNodes(
            selected: List<Long>,
            nodeList: List<NodeUIItem<TypedNode>>,
            menu: Menu,
        ) {
            val isHiddenNodesEnabled = getFeatureFlagValueUseCase(AppFeatures.HiddenNodes)
            if (!isHiddenNodesEnabled) {
                menu.findItem(R.id.cab_menu_hide)?.isVisible = false
                menu.findItem(R.id.cab_menu_unhide)?.isVisible = false
                return
            }

            val selectedNodes = selected.mapNotNull { nodeId ->
                nodeList.find { it.id.longValue == nodeId }
            }

            val isHidingActionAllowed = selected.all {
                fileBrowserViewModel.isHidingActionAllowed(NodeId(it))
            }

            if (!isHidingActionAllowed) {
                menu.findItem(R.id.cab_menu_hide)?.isVisible = false
                menu.findItem(R.id.cab_menu_unhide)?.isVisible = false
                return
            }
            val includeSensitiveInheritedNode = selectedNodes.any { it.isSensitiveInherited }

            val hasNonSensitiveNode = selectedNodes.any { !it.isMarkedSensitive }
            val isPaid = fileBrowserViewModel.state.value.accountType?.isPaid ?: false

            menu.findItem(R.id.cab_menu_hide)?.isVisible =
                !isPaid || (hasNonSensitiveNode && !includeSensitiveInheritedNode)
            menu.findItem(R.id.cab_menu_unhide)?.isVisible =
                isPaid && !hasNonSensitiveNode && !includeSensitiveInheritedNode
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            Timber.d("onActionItemClicked")
            fileBrowserViewModel.onOptionItemClicked(item)
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            fileBrowserViewModel.clearAllNodes()
            (activity as? ManagerActivity)?.let {
                it.showFabButton()
                it.showHideBottomNavigationView(false)
                it.handleShowingAds(TAB_CLOUD_SLOT_ID)
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
                    fileBrowserViewModel.state.value.selectedNodeHandles.takeIf { handles -> handles.isNotEmpty() }
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
                    fileBrowserViewModel.selectAllNodes()
                }

                OptionItems.CLEAR_ALL_CLICKED -> {
                    disableSelectMode()
                }

                OptionItems.HIDE_CLICKED -> {
                    Analytics.tracker.trackEvent(CloudDriveHideNodeMenuItemEvent)
                    handleHideNodeClick(
                        nodeIds = it.selectedMegaNode.map { node -> NodeId(node.handle) },
                    )
                    disableSelectMode()
                }

                OptionItems.UNHIDE_CLICKED -> {
                    fileBrowserViewModel.hideOrUnhideNodes(
                        nodeIds = it.selectedMegaNode.map { node -> NodeId(node.handle) },
                        hide = false
                    )
                    disableSelectMode()
                }

                OptionItems.COPY_CLICKED -> {
                    val nC = NodeController(requireActivity())
                    nC.chooseLocationToCopyNodes(fileBrowserViewModel.state.value.selectedNodeHandles)
                    disableSelectMode()
                }

                OptionItems.MOVE_CLICKED -> {
                    val nC = NodeController(requireActivity())
                    nC.chooseLocationToMoveNodes(fileBrowserViewModel.state.value.selectedNodeHandles)
                    disableSelectMode()
                }

                OptionItems.DISPUTE_CLICKED -> {
                    startActivity(
                        Intent(requireContext(), WebViewActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            .setData(Uri.parse(Constants.DISPUTE_URL))
                    )
                    disableSelectMode()
                }

                OptionItems.LEAVE_SHARE_CLICKED -> {
                    val handleList =
                        ArrayList<Long>().apply { addAll(it.selectedNode.map { node -> node.id.longValue }) }
                    MegaNodeUtil.showConfirmationLeaveIncomingShares(
                        requireActivity(),
                        (requireActivity() as SnackbarShower), handleList
                    )
                    disableSelectMode()
                }
            }
        }
    }

    /**
     * Clicked on link
     * @param link
     */
    private fun navigateToLink(link: String) {
        val uriUrl = Uri.parse(link)
        val launchBrowser = Intent(requireContext(), WebViewActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .setData(uriUrl)
        startActivity(launchBrowser)
    }

    private fun disableSelectMode() {
        fileBrowserViewModel.clearAllNodes()
        actionMode?.finish()
    }

    fun handleHideNodeClick(nodeIds: List<NodeId>) {
        val (isPaid, isHiddenNodesOnboarded) = with(fileBrowserViewModel.state.value) {
            (this.accountType?.isPaid ?: false) to this.isHiddenNodesOnboarded
        }


        if (!isPaid) {
            val intent = HiddenNodesOnboardingActivity.createScreen(
                context = requireContext(),
                isOnboarding = false,
            )
            hiddenNodesOnboardingLauncher.launch(intent)
            activity?.overridePendingTransition(0, 0)
        } else if (isHiddenNodesOnboarded) {
            fileBrowserViewModel.hideOrUnhideNodes(
                nodeIds = nodeIds,
                hide = true,
            )
        } else {
            tempNodeIds = nodeIds
            showHiddenNodesOnboarding()
        }
    }

    private fun showHiddenNodesOnboarding() {
        fileBrowserViewModel.setHiddenNodesOnboarded()

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

        fileBrowserViewModel.hideOrUnhideNodes(
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