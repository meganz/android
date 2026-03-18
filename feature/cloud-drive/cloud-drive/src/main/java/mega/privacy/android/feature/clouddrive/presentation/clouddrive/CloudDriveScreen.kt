package mega.privacy.android.feature.clouddrive.presentation.clouddrive

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.android.core.ui.modifiers.applyScrollToHideFabBehavior
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.action.rememberMultiNodeActionHandler
import mega.privacy.android.core.nodecomponents.components.AddContentFab
import mega.privacy.android.core.nodecomponents.components.selectionmode.NodeSelectionModeAppBar
import mega.privacy.android.core.nodecomponents.components.selectionmode.NodeSelectionModeBottomBar
import mega.privacy.android.core.nodecomponents.sheet.options.NodeOptionsBottomSheetNavKey
import mega.privacy.android.core.nodecomponents.upload.ScanDocumentHandler
import mega.privacy.android.core.nodecomponents.upload.ScanDocumentViewModel
import mega.android.core.ui.extensions.LaunchedOnceEffect
import mega.privacy.android.core.sharedcomponents.menu.CommonAppBarAction
import mega.privacy.android.core.transfers.widget.TransfersToolbarWidget
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveAction.DeselectAllItems
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.CloudDriveAction.SelectAllItems
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.model.searchNavKey
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.view.CloudDriveContent
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.TransfersNavKey
import mega.privacy.android.navigation.extensions.rememberMegaNavigator
import mega.privacy.mobile.analytics.event.CloudDriveBottomToolBarMoreMenuItemEvent
import mega.privacy.mobile.analytics.event.CloudDriveFABPressedEvent
import mega.privacy.mobile.analytics.event.CloudDriveParentNodeMoreButtonPressedEvent
import mega.privacy.mobile.analytics.event.CloudDriveScreenEvent

/**
 * Cloud Drive Screen, used to display contents of a folder
 *
 * @param onBack Callback to be invoked when the back button is pressed
 * @param onTransfer Callback to handle transfer events
 * @param viewModel ViewModel for managing the state of the Cloud Drive screen
 */
@SuppressLint("ComposeModifierMissing")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudDriveScreen(
    navigationHandler: NavigationHandler,
    onBack: () -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
    setNavigationBarVisibility: (Boolean) -> Unit,
    viewModel: CloudDriveViewModel = hiltViewModel(),
    nodeOptionsActionViewModel: NodeOptionsActionViewModel =
        hiltViewModel<NodeOptionsActionViewModel, NodeOptionsActionViewModel.Factory>(
            creationCallback = { it.create(NodeSourceType.CLOUD_DRIVE) }
        ),
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
    BackHandler(enabled = uiState.isInSelectionMode) {
        viewModel.processAction(DeselectAllItems)
    }

    LaunchedOnceEffect {
        Analytics.tracker.trackEvent(CloudDriveScreenEvent)
    }

    MegaScaffoldWithTopAppBarScrollBehavior(
        topBar = {
            if (uiState.isInSelectionMode) {
                NodeSelectionModeAppBar(
                    modifier = Modifier.testTag(CLOUD_DRIVE_SELECTION_MODE_APP_BAR_TAG),
                    count = uiState.selectedItemsCount,
                    isAllSelected = uiState.isAllSelected,
                    isSelecting = uiState.isSelecting,
                    onSelectAllClicked = { viewModel.processAction(SelectAllItems) },
                    onCancelSelectionClicked = { viewModel.processAction(DeselectAllItems) }
                )
            } else {
                MegaTopAppBar(
                    modifier = Modifier.testTag(CLOUD_DRIVE_MAIN_APP_BAR_TAG),
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
                                MenuActionWithClick(CommonAppBarAction.Search) {
                                    navigationHandler.navigate(uiState.searchNavKey)
                                }
                            )
                        }

                        if (!uiState.isCloudDriveRoot) {
                            add(
                                MenuActionWithClick(
                                    CommonAppBarAction.More
                                ) {
                                    Analytics.tracker.trackEvent(
                                        CloudDriveParentNodeMoreButtonPressedEvent
                                    )
                                    val folderId = uiState.currentFolderId.longValue
                                    if (folderId != -1L) {
                                        navigationHandler.navigate(
                                            NodeOptionsBottomSheetNavKey(
                                                nodeHandle = folderId,
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
        },
        bottomBar = {
            NodeSelectionModeBottomBar(
                modifier = Modifier.testTag(CLOUD_DRIVE_SELECTION_MODE_BOTTOM_BAR_TAG),
                availableActions = nodeOptionsActionUiState.availableActions,
                visibleActions = nodeOptionsActionUiState.visibleActions,
                visible = nodeOptionsActionUiState.visibleActions.isNotEmpty() && uiState.isInSelectionMode,
                multiNodeActionHandler = selectionModeActionHandler,
                selectedNodes = uiState.selectedNodes,
                isSelecting = uiState.isSelecting,
                onMoreClicked = {
                    Analytics.tracker.trackEvent(CloudDriveBottomToolBarMoreMenuItemEvent)
                }
            )
        },
        floatingActionButton = {
            AddContentFab(
                modifier = Modifier
                    .testTag(CLOUD_DRIVE_FAB_TAG)
                    .applyScrollToHideFabBehavior(),
                visible = uiState.isUploadAllowed && !uiState.isEmpty,
                onClick = {
                    Analytics.tracker.trackEvent(CloudDriveFABPressedEvent)
                    showUploadOptionsBottomSheet = true
                }
            )
        },
        content = { innerPadding ->
            CloudDriveContent(
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
                nodeOptionsActionViewModel = nodeOptionsActionViewModel,
            )
        }
    )

    LaunchedEffect(uiState.isInSelectionMode) {
        setNavigationBarVisibility(!uiState.isInSelectionMode)
    }

    @SuppressLint("ComposeViewModelForwarding")
    ScanDocumentHandler(
        parentNodeId = uiState.currentFolderId,
        megaNavigator = megaNavigator,
        viewModel = scanDocumentViewModel
    )
}

internal const val CLOUD_DRIVE_FAB_TAG = "cloud_drive_screen:add_content_fab"
internal const val CLOUD_DRIVE_MAIN_APP_BAR_TAG = "cloud_drive_screen:main_app_bar"
internal const val CLOUD_DRIVE_SELECTION_MODE_APP_BAR_TAG =
    "cloud_drive_screen:selection_mode_app_bar"
internal const val CLOUD_DRIVE_SELECTION_MODE_BOTTOM_BAR_TAG =
    "cloud_drive_screen:selection_mode_bottom_bar"