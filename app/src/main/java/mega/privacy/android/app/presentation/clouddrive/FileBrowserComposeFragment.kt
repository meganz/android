package mega.privacy.android.app.presentation.clouddrive

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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.WebViewActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.fragments.homepage.EventObserver
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.interfaces.ActionBackupListener
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
import mega.privacy.android.app.presentation.mapper.GetIntentToOpenFileMapper
import mega.privacy.android.app.presentation.mapper.GetOptionsForToolbarMapper
import mega.privacy.android.app.presentation.mapper.OptionsItemInfo
import mega.privacy.android.app.presentation.transfers.startdownload.view.StartDownloadComponent
import mega.privacy.android.app.sync.fileBackups.FileBackupManager
import mega.privacy.android.app.utils.CloudStorageOptionControlUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.MegaApiUtils
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.theme.MegaAppTheme
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
     * Mapper to open file
     */
    @Inject
    lateinit var getIntentToOpenFileMapper: GetIntentToOpenFileMapper

    /**
     * Mapper to get options for Action Bar
     */
    @Inject
    lateinit var getOptionsForToolbarMapper: GetOptionsForToolbarMapper

    private val fileBrowserViewModel: FileBrowserViewModel by activityViewModels()
    private val sortByHeaderViewModel: SortByHeaderViewModel by activityViewModels()

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
                val snackbarHostState = remember { SnackbarHostState() }
                val coroutineScope = rememberCoroutineScope()
                MegaAppTheme(isDark = themeMode.isDarkMode()) {
                    FileBrowserComposeView(
                        uiState = uiState,
                        emptyState = getEmptyFolderDrawable(uiState.isFileBrowserEmpty),
                        onItemClick = fileBrowserViewModel::onItemClicked,
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
                                    snackbarHostState.showSnackbar(
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
                    )

                    // Snackbar host state should be attached to snackbar host in the scaffold, but we don't have a scaffold yet
                    LaunchedEffect(snackbarHostState.currentSnackbarData) {
                        snackbarHostState.currentSnackbarData?.message?.let {
                            Util.showSnackbar(activity, it)
                        }
                    }
                    StartDownloadComponent(
                        uiState.downloadEvent,
                        {
                            fileBrowserViewModel.consumeDownloadEvent()
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
                onItemClick(uiState.currentFileNode)
                HandleMediaDiscoveryVisibility(
                    isMediaDiscoveryOpen = uiState.isMediaDiscoveryOpen,
                    isMediaDiscoveryOpenedByIconClick = uiState.isMediaDiscoveryOpenedByIconClick,
                    mediaHandle = uiState.fileBrowserHandle,
                )
                UpdateToolbarTitle(uiState.updateToolbarTitleEvent) {
                    fileBrowserViewModel.consumeUpdateToolbarTitleEvent()
                }
                ExitFileBrowser(uiState.exitFileBrowserEvent) {
                    fileBrowserViewModel.consumeExitFileBrowserEvent()
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
     */
    @Composable
    private fun HandleMediaDiscoveryVisibility(
        isMediaDiscoveryOpen: Boolean,
        isMediaDiscoveryOpenedByIconClick: Boolean,
        mediaHandle: Long,
    ) {
        if (isMediaDiscoveryOpen) {
            SideEffect {
                fileBrowserActionListener?.showMediaDiscoveryFromCloudDrive(
                    mediaHandle = mediaHandle,
                    isAccessedByIconClick = isMediaDiscoveryOpenedByIconClick,
                )
                fileBrowserViewModel.onItemPerformedClicked()
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
     * On Item click event received from [FileBrowserViewModel]
     *
     * @param currentFileNode [FileNode]
     */
    private fun onItemClick(currentFileNode: FileNode?) {
        currentFileNode?.let {
            openFile(fileNode = it)
            fileBrowserViewModel.onItemPerformedClicked()
        } ?: run {
            fileBrowserActionListener?.updateCloudDriveToolbarTitle(invalidateOptionsMenu = false)
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
     * onViewCreated
     */
    @OptIn(FlowPreview::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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

        sortByHeaderViewModel.orderChangeEvent.observe(viewLifecycleOwner, EventObserver {
            fileBrowserViewModel.refreshNodes()
        })
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
            menu.findItem(R.id.cab_menu_share_link).title =
                resources.getQuantityString(R.plurals.get_links, selected.size)
            lifecycleScope.launch {
                val control = getOptionsForToolbarMapper(
                    selectedNodeHandleList = fileBrowserViewModel.state.value.selectedNodeHandles,
                    totalNodes = fileBrowserViewModel.state.value.nodesList.size
                )
                CloudStorageOptionControlUtil.applyControl(menu, control)
            }
            return true
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
                        fromMediaViewer = false,
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
            }
        }
    }

    /**
     * Open File
     * @param fileNode [FileNode]
     */
    private fun openFile(fileNode: FileNode) {
        lifecycleScope.launch {
            runCatching {
                val intent = getIntentToOpenFileMapper(
                    activity = requireActivity(),
                    fileNode = fileNode,
                    viewType = Constants.FILE_BROWSER_ADAPTER
                )
                intent?.let {
                    if (MegaApiUtils.isIntentAvailable(context, it)) {
                        startActivity(it)
                    } else {
                        Toast.makeText(
                            context,
                            getString(R.string.intent_not_available),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }.onFailure {
                Timber.e("itemClick:ERROR:httpServerGetLocalLink")
                (activity as? BaseActivity)?.showSnackbar(
                    type = Constants.SNACKBAR_TYPE,
                    content = getString(R.string.general_text_error),
                    chatId = -1,
                )
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
}