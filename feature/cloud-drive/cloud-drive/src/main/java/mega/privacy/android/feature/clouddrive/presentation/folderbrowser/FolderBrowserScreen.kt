package mega.privacy.android.feature.clouddrive.presentation.folderbrowser

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.extensions.LaunchedOnceEffect
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.android.core.ui.modifiers.applyScrollToHideFabBehavior
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.action.rememberMultiNodeActionHandler
import mega.privacy.android.core.nodecomponents.components.AddContentFab
import mega.privacy.android.core.nodecomponents.components.selectionmode.NodeSelectionActionsEffect
import mega.privacy.android.core.nodecomponents.components.selectionmode.NodeSelectionModeBottomBar
import mega.privacy.android.core.nodecomponents.sheet.options.NodeOptionsBottomSheetNavKey
import mega.privacy.android.core.nodecomponents.upload.ScanDocumentHandler
import mega.privacy.android.core.nodecomponents.upload.ScanDocumentViewModel
import mega.privacy.android.core.transfers.widget.TransfersToolbarWidget
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.clouddrive.presentation.folderbrowser.view.FolderBrowserContent
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.menu.CommonMenuAction
import mega.privacy.android.navigation.contract.state.ReportSelectionMode
import mega.privacy.android.navigation.destination.SearchNavKey
import mega.privacy.android.navigation.destination.TransfersNavKey
import mega.privacy.android.navigation.extensions.rememberMegaNavigator
import mega.privacy.android.shared.nodes.components.NodeSelectionModeAppBar
import mega.privacy.android.shared.nodes.selection.rememberNodeSelection
import mega.privacy.mobile.analytics.event.CloudDriveBottomToolBarMoreMenuItemEvent
import mega.privacy.mobile.analytics.event.CloudDriveFABPressedEvent
import mega.privacy.mobile.analytics.event.CloudDriveParentNodeMoreButtonPressedEvent
import mega.privacy.mobile.analytics.event.CloudDriveScreenEvent

/**
 * Shows the contents of a folder page by page. A copy of `CloudDriveScreen` for the paged stack;
 * a fix to either during the flag window must be mirrored in the other.
 *
 * @param onBack invoked when the back button is pressed
 * @param onTransfer handles transfer events
 */
@SuppressLint("ComposeModifierMissing")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderBrowserScreen(
    navigationHandler: NavigationHandler,
    onBack: () -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
    navigateToCloudDriveFolder: (TypedFolderNode, NodeSourceType) -> Unit,
    setNavigationBarVisibility: (Boolean) -> Unit,
    nodeOptionsActionViewModel: NodeOptionsActionViewModel,
    viewModel: FolderBrowserViewModel = hiltViewModel(),
    scanDocumentViewModel: ScanDocumentViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showUploadOptionsBottomSheet by rememberSaveable { mutableStateOf(false) }
    val megaNavigator = rememberMegaNavigator()
    val nodeOptionsActionUiState by nodeOptionsActionViewModel.uiState.collectAsStateWithLifecycle()
    val selectionModeActionHandler = rememberMultiNodeActionHandler(
        navigationHandler = navigationHandler,
        viewModel = nodeOptionsActionViewModel,
        megaNavigator = megaNavigator,
    )

    val selection = rememberNodeSelection(
        items = uiState.items,
        loadingState = uiState.nodesLoadingState,
    )
    ReportSelectionMode(isInSelectionMode = selection.isInSelectionMode)
    NodeSelectionActionsEffect(
        selection = selection,
        nodeSourceType = uiState.nodeSourceType,
        uiState = nodeOptionsActionUiState,
        onIntent = nodeOptionsActionViewModel::processIntent,
    )

    BackHandler(enabled = selection.isInSelectionMode) {
        selection.deselectAll()
    }

    LaunchedOnceEffect {
        Analytics.tracker.trackEvent(CloudDriveScreenEvent)
    }

    MegaScaffoldWithTopAppBarScrollBehavior(
        topBar = {
            val folderId = uiState.currentFolderId
            when {
                uiState.isLoading || folderId == null -> {
                    MegaTopAppBar(
                        modifier = Modifier.testTag(FOLDER_BROWSER_MAIN_APP_BAR_TAG),
                        title = uiState.title.text,
                        navigationType = AppBarNavigationType.Back(onBack),
                        trailingIcons = {
                            TransfersToolbarWidget {
                                navigationHandler.navigate(TransfersNavKey())
                            }
                        }
                    )
                }

                selection.isInSelectionMode -> {
                    NodeSelectionModeAppBar(
                        modifier = Modifier.testTag(FOLDER_BROWSER_SELECTION_MODE_APP_BAR_TAG),
                        selection = selection,
                    )
                }

                else -> {
                    MegaTopAppBar(
                        modifier = Modifier.testTag(FOLDER_BROWSER_MAIN_APP_BAR_TAG),
                        title = uiState.title.text,
                        navigationType = AppBarNavigationType.Back(onBack),
                        trailingIcons = {
                            TransfersToolbarWidget {
                                navigationHandler.navigate(TransfersNavKey())
                            }
                        },
                        actions = buildList {
                            if (uiState.items.isNotEmpty()) {
                                add(
                                    MenuActionWithClick(CommonMenuAction.Search) {
                                        navigationHandler.navigate(
                                            SearchNavKey(
                                                parentHandle = folderId.longValue,
                                                nodeSourceType = uiState.nodeSourceType
                                            )
                                        )
                                    }
                                )
                            }

                            if (!uiState.isCloudDriveRoot) {
                                add(
                                    MenuActionWithClick(CommonMenuAction.More) {
                                        Analytics.tracker.trackEvent(
                                            CloudDriveParentNodeMoreButtonPressedEvent
                                        )
                                        if (folderId.longValue != -1L) {
                                            navigationHandler.navigate(
                                                NodeOptionsBottomSheetNavKey(
                                                    nodeHandle = folderId.longValue,
                                                    nodeSourceType = uiState.nodeSourceType,
                                                )
                                            )
                                        }
                                    }
                                )
                            }
                        },
                    )
                }
            }
        },
        bottomBar = {
            @SuppressLint("ComposeViewModelForwarding")
            NodeSelectionModeBottomBar(
                modifier = Modifier.testTag(FOLDER_BROWSER_SELECTION_MODE_BOTTOM_BAR_TAG),
                availableActions = nodeOptionsActionUiState.availableActions,
                visibleActions = nodeOptionsActionUiState.visibleActions,
                visible = nodeOptionsActionUiState.visibleActions.isNotEmpty() && selection.isInSelectionMode,
                multiNodeActionHandler = selectionModeActionHandler,
                selectedNodes = selection.selectedNodes,
                isSelecting = selection.isSelecting,
                onMoreClicked = {
                    Analytics.tracker.trackEvent(CloudDriveBottomToolBarMoreMenuItemEvent)
                }
            )
        },
        floatingActionButton = {
            val visible = !uiState.isLoading &&
                    uiState.isUploadAllowed &&
                    uiState.items.isNotEmpty() &&
                    !selection.isInSelectionMode
            AddContentFab(
                modifier = Modifier
                    .testTag(FOLDER_BROWSER_FAB_TAG)
                    .applyScrollToHideFabBehavior(),
                visible = visible,
                onClick = {
                    Analytics.tracker.trackEvent(CloudDriveFABPressedEvent)
                    showUploadOptionsBottomSheet = true
                }
            )
        },
        content = { innerPadding ->
            FolderBrowserContent(
                isTabContent = false,
                navigationHandler = navigationHandler,
                uiState = uiState,
                showUploadOptionsBottomSheet = showUploadOptionsBottomSheet,
                onToggleShowUploadOptionsBottomSheet = { showUploadOptionsBottomSheet = it },
                contentPadding = innerPadding,
                onAction = viewModel::processAction,
                onPrepareScanDocument = scanDocumentViewModel::prepareDocumentScanner,
                onNavigateBack = onBack,
                onTransfer = onTransfer,
                onSortNodes = viewModel::setCloudSortOrder,
                selection = selection,
                navigateToFolder = navigateToCloudDriveFolder,
            )
        }
    )

    LaunchedEffect(selection.isInSelectionMode) {
        setNavigationBarVisibility(!selection.isInSelectionMode)
    }

    uiState.currentFolderId?.let {
        @SuppressLint("ComposeViewModelForwarding")
        ScanDocumentHandler(
            parentNodeId = it,
            navigate = navigationHandler::navigate,
            viewModel = scanDocumentViewModel
        )
    }
}

internal const val FOLDER_BROWSER_FAB_TAG = "folder_browser_screen:add_content_fab"
internal const val FOLDER_BROWSER_MAIN_APP_BAR_TAG = "folder_browser_screen:main_app_bar"
internal const val FOLDER_BROWSER_SELECTION_MODE_APP_BAR_TAG =
    "folder_browser_screen:selection_mode_app_bar"
internal const val FOLDER_BROWSER_SELECTION_MODE_BOTTOM_BAR_TAG =
    "folder_browser_screen:selection_mode_bottom_bar"
