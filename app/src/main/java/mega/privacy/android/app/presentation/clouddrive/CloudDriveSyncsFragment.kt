package mega.privacy.android.app.presentation.clouddrive

import android.app.Activity
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
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import de.palm.composestateevents.EventEffect
import de.palm.composestateevents.StateEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.extensions.launchUrl
import mega.privacy.android.app.features.CloudDriveFeature
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.interfaces.ActionBackupListener
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.main.dialog.removelink.RemovePublicLinkDialogFragment
import mega.privacy.android.app.main.dialog.rubbishbin.ConfirmMoveToRubbishBinDialogFragment
import mega.privacy.android.app.main.dialog.shares.RemoveAllSharingContactDialogFragment
import mega.privacy.android.app.main.share.TAB_ROW_TEST_TAG
import mega.privacy.android.app.presentation.bottomsheet.NodeOptionsBottomSheetDialogFragment
import mega.privacy.android.app.presentation.clouddrive.model.FileBrowserState
import mega.privacy.android.app.presentation.clouddrive.ui.FileBrowserComposeView
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.hidenode.HiddenNodesOnboardingActivity
import mega.privacy.android.app.presentation.mapper.GetOptionsForToolbarMapper
import mega.privacy.android.app.presentation.mapper.OptionsItemInfo
import mega.privacy.android.app.presentation.node.action.HandleNodeAction
import mega.privacy.android.app.presentation.photos.albums.add.AddToAlbumActivity
import mega.privacy.android.app.presentation.qrcode.findActivity
import mega.privacy.android.app.presentation.settings.model.storageTargetPreference
import mega.privacy.android.app.presentation.transfers.TransfersManagementViewModel
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
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.feature.sync.ui.SyncIssueNotificationViewModel
import mega.privacy.android.feature.sync.ui.permissions.SyncPermissionsManager
import mega.privacy.android.feature.sync.ui.synclist.SyncListRoute
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.controls.tab.Tabs
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.CloudDriveHideNodeMenuItemEvent
import mega.privacy.mobile.analytics.event.CloudDriveScreenEvent
import mega.privacy.mobile.analytics.event.SyncsTabEvent
import timber.log.Timber
import javax.inject.Inject

/**
 * A Fragment for Cloud Drive and Syncs
 */
@AndroidEntryPoint
class CloudDriveSyncsFragment : Fragment() {

    companion object {
        /**
         * Returns the instance of CloudDriveSyncsFragment
         */
        @JvmStatic
        fun newInstance() = CloudDriveSyncsFragment()

        /**
         * Returns the strings of the tabs
         */
        val tabResIds = listOf(
            R.string.section_cloud_drive,
            sharedR.string.general_syncs
        )
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
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

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

    /**
     * App Navigator
     */
    @Inject
    lateinit var appNavigator: MegaNavigator

    private val fileBrowserViewModel: FileBrowserViewModel by activityViewModels()
    private val sortByHeaderViewModel: SortByHeaderViewModel by activityViewModels()
    private val transfersManagementViewModel: TransfersManagementViewModel by activityViewModels()
    private val syncIssueNotificationViewModel: SyncIssueNotificationViewModel by activityViewModels()

    private var tempNodeIds: List<NodeId> = listOf()

    /**
     * Allows navigation to specific features in the monolith :app
     */
    @Inject
    lateinit var megaNavigator: MegaNavigator

    /**
     * getFeatureFlagValueUseCase
     */
    @Inject
    lateinit var getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase

    /**
     * syncPermissionsManager
     */
    @Inject
    lateinit var syncPermissionsManager: SyncPermissionsManager

    /**
     * Mega node util wrapper
     */
    @Inject
    lateinit var megaNodeUtilWrapper: MegaNodeUtilWrapper

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
            },
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            megaNodeUtilWrapper = megaNodeUtilWrapper,
        )
    }

    /**
     * onCreateView
     */
    @OptIn(ExperimentalMaterial3Api::class)
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
                val uiState by fileBrowserViewModel.state.collectAsStateWithLifecycle()
                val syncNotificationState by syncIssueNotificationViewModel.state.collectAsStateWithLifecycle()
                val scaffoldState = rememberScaffoldState()
                val snackbarHostState = scaffoldState.snackbarHostState
                val coroutineScope = rememberCoroutineScope()
                var clickedFile: TypedFileNode? by remember {
                    mutableStateOf(null)
                }
                val pagerState =
                    rememberPagerState(initialPage = uiState.selectedTab.position.takeIf { it != CloudDriveTab.NONE.position }
                        ?: 0) { 2 }
                LaunchedEffect(pagerState.currentPage) {
                    if (pagerState.currentPage == CloudDriveTab.SYNC.position) {
                        Analytics.tracker.trackEvent(SyncsTabEvent)
                    }
                    fileBrowserViewModel.onTabChanged(CloudDriveTab.entries.first { it.position == pagerState.currentPage })
                }
                LaunchedEffect(uiState.selectedTab) {
                    pagerState.scrollToPage(uiState.selectedTab.position)
                }

                // Restore action mode when selection state changes (e.g., after device rotation)
                LaunchedEffect(uiState.isInSelection, uiState.selectedNodeHandles.size) {
                    if (uiState.isInSelection && uiState.selectedNodeHandles.isNotEmpty()) {
                        if (actionMode == null) {
                            actionMode =
                                (requireActivity() as? AppCompatActivity)?.startSupportActionMode(
                                    ActionBarCallBack()
                                )
                        }
                    } else {
                        actionMode?.finish()
                        actionMode = null
                    }
                }

                val isTabShown = when (uiState.selectedTab) {
                    CloudDriveTab.CLOUD -> uiState.isRootNode && !uiState.isInSelection
                    else -> true
                }

                OriginalTheme(isDark = themeMode.isDarkMode()) {
                    MegaScaffold(
                        scaffoldState = scaffoldState,
                        topBar = {
                            Tabs(
                                modifier = Modifier.testTag(TAB_ROW_TEST_TAG),
                                shouldTabsShown = isTabShown,
                                pagerState = pagerState,
                            ) {
                                CloudDriveTab.entries.filter { it != CloudDriveTab.NONE }
                                    .forEachIndexed { page, item ->
                                        addTextTab(
                                            text = stringResource(tabResIds[page]),
                                            tag = item.name,
                                            suffix = { activeColor, color ->
                                                if (item == CloudDriveTab.SYNC && syncNotificationState.syncNotificationType != null) {
                                                    Box(
                                                        modifier = Modifier
                                                            .offset(6.dp, 0.dp)
                                                            .size(4.dp)
                                                            .background(
                                                                color = activeColor,
                                                                shape = CircleShape
                                                            )
                                                    )
                                                }
                                            }
                                        )
                                    }
                            }
                        }
                    ) {
                        HorizontalPager(
                            modifier = Modifier.fillMaxWidth(),
                            state = pagerState,
                            verticalAlignment = Alignment.Top,
                            userScrollEnabled = isTabShown,
                        ) { page: Int ->
                            val activity = LocalContext.current.findActivity() as AppCompatActivity
                            when (page) {
                                CloudDriveTab.CLOUD.position -> {
                                    CloudDriveTab(
                                        uiState = uiState,
                                        snackbarHostState = snackbarHostState,
                                        coroutineScope = coroutineScope,
                                        fileTypeIconMapper = fileTypeIconMapper,
                                        onClickedFile = {
                                            clickedFile = it
                                        }
                                    )
                                }

                                CloudDriveTab.SYNC.position -> {
                                    SyncListRoute(
                                        isInCloudDrive = true,
                                        syncPermissionsManager = syncPermissionsManager,
                                        onSyncFolderClicked = {
                                            megaNavigator.openNewSync(
                                                activity,
                                                SyncType.TYPE_TWOWAY,
                                                isFromCloudDrive = true
                                            )
                                        },
                                        onBackupFolderClicked = {
                                            megaNavigator.openNewSync(
                                                activity,
                                                SyncType.TYPE_BACKUP,
                                                isFromCloudDrive = true
                                            )
                                        },
                                        onSelectStopBackupDestinationClicked = { folderName ->
                                            megaNavigator.openSelectStopBackupDestinationFromSyncsTab(
                                                activity,
                                                folderName
                                            )
                                        },
                                        onOpenUpgradeAccountClicked = {
                                            megaNavigator.openUpgradeAccount(activity)
                                        },
                                        onOpenMegaFolderClicked = {
                                            coroutineScope.launch {
                                                fileBrowserViewModel.setIsFromSyncTab(true)
                                                appNavigator.openNodeInCloudDrive(
                                                    activity,
                                                    nodeHandle = it,
                                                    errorMessage = null,
                                                    isFromSyncFolders = false
                                                )
                                            }
                                        },
                                        onCameraUploadsSettingsClicked = {
                                            megaNavigator.openSettingsCameraUploads(activity)
                                        },
                                        onFabExpanded = { isExpanded ->
                                            if (isExpanded) {
                                                transfersManagementViewModel.hideTransfersWidget()
                                            } else {
                                                transfersManagementViewModel.showTransfersWidget()
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        StartTransferComponent(
                            uiState.downloadEvent,
                            {
                                fileBrowserViewModel.consumeDownloadEvent()
                                disableSelectMode()
                            },
                            snackBarHostState = snackbarHostState,
                            navigateToStorageSettings = {
                                megaNavigator.openSettings(
                                    requireActivity(),
                                    storageTargetPreference
                                )
                            },
                        )
                    }


                    val modalSheetState = rememberModalBottomSheetState(
                        skipPartiallyExpanded = true
                    )
                    if (uiState.showColoredFoldersOnboarding) {
                        MegaModalBottomSheet(
                            sheetState = modalSheetState,
                            bottomSheetBackground = MegaModalBottomSheetBackground.Surface1,
                            onDismissRequest = {
                                fileBrowserViewModel.onColoredFoldersOnboardingDismissed()
                            }
                        ) {
                            ColoredFoldersOnboardingBottomSheet(
                                onDismiss = {
                                    coroutineScope.launch {
                                        modalSheetState.hide()
                                        fileBrowserViewModel.onColoredFoldersOnboardingDismissed()
                                    }
                                }
                            )
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
                        nodeSourceType = NodeSourceTypeInt.FILE_BROWSER_ADAPTER,
                        sortOrder = uiState.sortOrder,
                        snackBarHostState = snackbarHostState,
                        onActionHandled = {
                            clickedFile = null
                        },
                        onDownloadEvent = fileBrowserViewModel::onDownloadFileTriggered,
                        coroutineScope = coroutineScope
                    )
                }
            }
        }
    }

    @Composable
    private fun CloudDriveTab(
        uiState: FileBrowserState,
        snackbarHostState: SnackbarHostState,
        coroutineScope: CoroutineScope,
        fileTypeIconMapper: FileTypeIconMapper,
        onClickedFile: (TypedFileNode) -> Unit,
    ) {
        FileBrowserComposeView(
            uiState = uiState,
            emptyState = getEmptyFolderDrawable(uiState.isFileBrowserEmpty),
            onItemClick = {
                if (uiState.selectedNodeHandles.isEmpty()) {
                    when (it.node) {
                        is TypedFileNode -> onClickedFile(it.node)

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
            onLinkClicked = context::launchUrl,
            onDisputeTakeDownClicked = context::launchUrl,
            onDismissClicked = fileBrowserViewModel::onBannerDismissClicked,
            onStorageAlmostFullWarningDismiss = fileBrowserViewModel::setStorageCapacityAsDefault,
            onUpgradeClicked = {
                fileBrowserViewModel::onBannerDismissClicked
                megaNavigator.openUpgradeAccount(requireContext())
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
                    replaceFragment = fileBrowserViewModel.state().hasNoOpenedFolders && fileBrowserViewModel.isFromSyncTab()
                        .not(),
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
            Pair(iconPackR.drawable.ic_empty_cloud_glass, R.string.context_empty_cloud_drive)
        } else {
            Pair(iconPackR.drawable.ic_empty_folder_glass, R.string.file_browser_empty_folder_new)
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
                it.hideAdsView()
                it.showHideBottomNavigationView(true)
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

                    handleHiddenNodes(selected, nodeList, menu)
                    handleAddToAlbum(selected, nodeList, menu)
                    handleFavourites(selected, nodeList, menu)
                    handleLabels(selected, nodeList, menu)
                }.onFailure {
                    Timber.e(it)
                }
            }
            return true
        }

        private suspend fun isHiddenNodesActive(): Boolean {
            val result = runCatching {
                getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)
            }
            return result.getOrNull() ?: false
        }

        private suspend fun handleHiddenNodes(
            selected: List<Long>,
            nodeList: List<NodeUIItem<TypedNode>>,
            menu: Menu,
        ) {
            val isHiddenNodesEnabled = isHiddenNodesActive()
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
            val isBusinessAccountExpired =
                fileBrowserViewModel.state.value.isBusinessAccountExpired

            menu.findItem(R.id.cab_menu_hide)?.isVisible =
                !isPaid || isBusinessAccountExpired || (hasNonSensitiveNode && !includeSensitiveInheritedNode)
            menu.findItem(R.id.cab_menu_unhide)?.isVisible =
                isPaid && !isBusinessAccountExpired && !hasNonSensitiveNode && !includeSensitiveInheritedNode
        }

        private fun handleAddToAlbum(
            selected: List<Long>,
            nodeList: List<NodeUIItem<TypedNode>>,
            menu: Menu,
        ) {
            val mediaNodes = nodeList
                .filter { it.id.longValue in selected }
                .filter {
                    val type = (it.node as? FileNode)?.type
                    type is ImageFileTypeInfo || type is VideoFileTypeInfo
                }

            if (mediaNodes.size == selected.size) {
                if (mediaNodes.all { (it.node as? FileNode)?.type is VideoFileTypeInfo }) {
                    menu.findItem(R.id.cab_menu_add_to_album)?.isVisible = false
                    menu.findItem(R.id.cab_menu_add_to)?.isVisible = true
                } else {
                    menu.findItem(R.id.cab_menu_add_to_album)?.isVisible = true
                    menu.findItem(R.id.cab_menu_add_to)?.isVisible = false
                }
            } else {
                menu.findItem(R.id.cab_menu_add_to_album)?.isVisible = false
                menu.findItem(R.id.cab_menu_add_to)?.isVisible = false
            }
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
                it.handleShowingAds()
                actionMode = null
            }
        }

        private suspend fun handleFavourites(
            selected: List<Long>,
            nodeList: List<NodeUIItem<TypedNode>>,
            menu: Menu,
        ) {
            // Check if the bugfix for allowing favorite in multiple selection is enabled
            val isFavoriteMultipleSelectionEnabled = runCatching {
                getFeatureFlagValueUseCase(CloudDriveFeature.FAVORITE_MULTIPLE_SELECTION)
            }.getOrNull() ?: false

            // Only show favorite options when the feature is enabled
            if (!isFavoriteMultipleSelectionEnabled) {
                menu.findItem(R.id.cab_menu_add_favourites)?.isVisible = false
                menu.findItem(R.id.cab_menu_remove_favourites)?.isVisible = false
                return
            }

            val selectedNodes = nodeList.filter { it.id.longValue in selected }

            // Count how many are favorites and how many are not
            val favouriteCount = selectedNodes.count { it.node.isFavourite }
            val nonFavouriteCount = selectedNodes.size - favouriteCount

            // Show "Add to favourites" if any item is not a favourite
            menu.findItem(R.id.cab_menu_add_favourites)?.isVisible = nonFavouriteCount > 0

            // Show "Remove from favourites" if all items are favourites
            menu.findItem(R.id.cab_menu_remove_favourites)?.isVisible =
                favouriteCount == selectedNodes.size && selectedNodes.isNotEmpty()
        }

        private suspend fun handleLabels(
            selected: List<Long>,
            nodeList: List<NodeUIItem<TypedNode>>,
            menu: Menu,
        ) {
            // Check if the feature for allowing label in multiple selection is enabled
            val isLabelMultipleSelectionEnabled = runCatching {
                getFeatureFlagValueUseCase(CloudDriveFeature.LABEL_MULTIPLE_SELECTION)
            }
                .onFailure { Timber.w(it, "Label multi-select flag check failed") }
                .getOrElse { false }

            // Only show label options when the feature is enabled
            val menuItem = menu.findItem(R.id.cab_menu_add_label)
            if (!isLabelMultipleSelectionEnabled) {
                menuItem?.isVisible = false
                return
            }

            val selectedNodes = nodeList.filter { it.id.longValue in selected }

            // Always show "Add label" when multiple selection is enabled and nodes are selected
            // The label dialog will handle both adding and removing labels
            menuItem?.isVisible = selectedNodes.isNotEmpty()
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
                    val nodeIds = it.selectedMegaNode.map { node -> NodeId(node.handle) }
                    fileBrowserViewModel.hideOrUnhideNodes(
                        nodeIds = nodeIds,
                        hide = false
                    )
                    val message =
                        resources.getQuantityString(
                            sharedR.plurals.unhidden_nodes_result_message,
                            nodeIds.size,
                            nodeIds.size,
                        )
                    Util.showSnackbar(requireActivity(), message)
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
                    context.launchUrl(Constants.DISPUTE_URL)
                    disableSelectMode()
                }

                // This option is only available in the Incoming Shares page
                OptionItems.LEAVE_SHARE_CLICKED -> Unit

                OptionItems.ADD_TO_ALBUM -> {
                    val intent = Intent(requireContext(), AddToAlbumActivity::class.java).apply {
                        val ids = it.selectedNode.map { node -> node.id.longValue }.toTypedArray()
                        putExtra("ids", ids)
                        putExtra("type", 0)
                    }
                    addToAlbumLauncher.launch(intent)
                    disableSelectMode()
                }

                OptionItems.ADD_TO -> {
                    val intent = Intent(requireContext(), AddToAlbumActivity::class.java).apply {
                        val ids = it.selectedNode.map { node -> node.id.longValue }.toTypedArray()
                        putExtra("ids", ids)
                        putExtra("type", 1)
                    }
                    addToAlbumLauncher.launch(intent)
                    disableSelectMode()
                }

                OptionItems.ADD_TO_FAVOURITES_CLICKED -> {
                    // Add selected nodes to favourites
                    fileBrowserViewModel.updateNodesFavorite(
                        nodes = it.selectedNode,
                        isFavorite = true
                    )
                    disableSelectMode()
                }

                OptionItems.REMOVE_FROM_FAVOURITES_CLICKED -> {
                    // Remove selected nodes from favourites
                    fileBrowserViewModel.updateNodesFavorite(
                        nodes = it.selectedNode,
                        isFavorite = false
                    )
                    disableSelectMode()
                }

                OptionItems.ADD_LABEL_CLICKED -> {
                    parentFragmentManager.setFragmentResultListener(
                        "labels_applied",
                        viewLifecycleOwner
                    ) { _, _ ->
                        disableSelectMode()
                    }
                    // Show label selection dialog for multiple nodes
                    showMultipleLabelSelectionDialog(it.selectedNode)
                }
            }
        }
    }

    private fun disableSelectMode() {
        fileBrowserViewModel.clearAllNodes()
        actionMode?.finish()
    }

    /**
     * Shows a dialog to select a label for multiple nodes
     */
    private fun showMultipleLabelSelectionDialog(selectedNodes: List<TypedNode>) {
        if (selectedNodes.isEmpty()) return

        // Use the existing NodeLabelBottomSheetDialogFragment with multiple nodes
        val nodeIds = selectedNodes.map { it.id }
        (requireActivity() as ManagerActivity).showNodeLabelsPanel(nodeIds)
    }

    /**
     * Handles the click event for hiding nodes in the file browser.
     *
     * This function determines the appropriate action to take when a user attempts to hide nodes,
     * based on their account status and whether they have completed the hidden nodes onboarding.
     *
     * @param nodeIds The list of [NodeId]s representing the nodes to be hidden.
     *
     */
    fun handleHideNodeClick(nodeIds: List<NodeId>) {
        val state = fileBrowserViewModel.state.value
        val isPaid = state.accountType?.isPaid ?: false
        val isHiddenNodesOnboarded = state.isHiddenNodesOnboarded
        val isBusinessAccountExpired = state.isBusinessAccountExpired


        if (!isPaid || isBusinessAccountExpired) {
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
            val message =
                resources.getQuantityString(
                    R.plurals.hidden_nodes_result_message,
                    nodeIds.size,
                    nodeIds.size,
                )
            Util.showSnackbar(requireActivity(), message)
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

    private val addToAlbumLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            ::handleAddToAlbumResult,
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

    private fun handleAddToAlbumResult(result: ActivityResult) {
        if (result.resultCode != Activity.RESULT_OK) return
        val message = result.data?.getStringExtra("message") ?: return

        Util.showSnackbar(requireActivity(), message)
    }
}
