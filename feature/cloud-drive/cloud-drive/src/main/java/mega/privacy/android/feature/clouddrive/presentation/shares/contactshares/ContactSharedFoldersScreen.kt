package mega.privacy.android.feature.clouddrive.presentation.shares.contactshares

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.modifiers.rememberScrollToHideBehavior
import mega.privacy.android.core.nodecomponents.action.MultiNodeActionHandler
import mega.privacy.android.core.nodecomponents.components.selectionmode.NodeSelectionModeBottomBar
import mega.privacy.android.core.nodecomponents.model.NodeActionState
import mega.privacy.android.core.transfers.widget.TransfersToolbarWidget
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.view.rememberScrollUpVisibility
import mega.privacy.android.feature.clouddrive.presentation.shares.contactshares.model.ContactSharedFoldersUiState
import mega.privacy.android.feature.clouddrive.presentation.shares.incomingshares.IncomingSharesContent
import mega.privacy.android.feature.clouddrive.presentation.shares.incomingshares.model.IncomingSharesAction
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.shared.nodes.components.NodeSelectionModeAppBar
import mega.privacy.android.shared.nodes.components.SortBottomSheet
import mega.privacy.android.shared.nodes.components.SortBottomSheetResult
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.NodeSortOption
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Screen listing the incoming shared folders for a single contact.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ContactSharedFoldersScreen(
    email: String,
    navigationHandler: NavigationHandler,
    onBack: () -> Unit,
    onNavigateToTransfers: () -> Unit,
    onShowNodeOptions: (NodeId) -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
    uiState: ContactSharedFoldersUiState,
    nodeActionState: NodeActionState,
    selectionModeActionHandler: MultiNodeActionHandler,
    onSelectAllClicked: () -> Unit,
    onCancelSelectionClicked: () -> Unit,
    onIncomingShareAction: (IncomingSharesAction) -> Unit,
    setSortOrder: (NodeSortConfiguration) -> Unit,
) {

    val sharesState = uiState.shares
    val isInSelectionMode = sharesState.isInSelectionMode
    val selectedItemsCount = sharesState.selectedItemsCount

    val sortBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSortBottomSheet by rememberSaveable { mutableStateOf(false) }
    val scrollToHideBehavior = rememberScrollToHideBehavior()
    val showNodeHeader = rememberScrollUpVisibility(scrollToHideBehavior)

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
        scrollToHideBehavior = scrollToHideBehavior,
        topBar = {
            if (isInSelectionMode) {
                NodeSelectionModeAppBar(
                    modifier = Modifier.testTag(CONTACT_SHARED_FOLDERS_SELECTION_MODE_APP_BAR_TAG),
                    count = selectedItemsCount,
                    isAllSelected = sharesState.isAllSelected,
                    isSelecting = false,
                    onSelectAllClicked = onSelectAllClicked,
                    onCancelSelectionClicked = onCancelSelectionClicked,
                )
            } else {
                MegaTopAppBar(
                    modifier = Modifier.testTag(CONTACT_SHARED_FOLDERS_APP_BAR_TAG),
                    navigationType = AppBarNavigationType.Back {
                        onBack()
                    },
                    title = uiState.contactName ?: email,
                    trailingIcons = {
                        TransfersToolbarWidget(
                            modifier = Modifier.testTag(CONTACT_SHARED_FOLDERS_TRANSFERS_WIDGET_TAG),
                        ) {
                            onNavigateToTransfers()
                        }
                    },
                )
            }
        },
        bottomBar = {
            @SuppressLint("ComposeViewModelForwarding")
            NodeSelectionModeBottomBar(
                modifier = Modifier.testTag(CONTACT_SHARED_FOLDERS_SELECTION_MODE_BOTTOM_BAR_TAG),
                availableActions = nodeActionState.availableActions,
                visibleActions = nodeActionState.visibleActions,
                visible = nodeActionState.visibleActions.isNotEmpty() && isInSelectionMode,
                multiNodeActionHandler = selectionModeActionHandler,
                selectedNodes = if (isInSelectionMode) sharesState.selectedNodes else emptyList(),
                isSelecting = false,
            )
        },
    ) { paddingValues ->
        IncomingSharesContent(
            modifier = Modifier.testTag(CONTACT_SHARED_FOLDERS_CONTENT_TAG),
            uiState = sharesState,
            navigationHandler = navigationHandler,
            onAction = onIncomingShareAction,
            onShowNodeOptions = onShowNodeOptions,
            onSortOrderClick = { showSortBottomSheet = true },
            showNodeHeader = showNodeHeader,
            contentPadding = paddingValues,
        )
    }



    if (showSortBottomSheet) {
        SortBottomSheet(
            title = stringResource(sharedR.string.action_sort_by_header),
            options = NodeSortOption.getOptionsForSourceType(NodeSourceType.INCOMING_SHARES),
            sheetState = sortBottomSheetState,
            selectedSort = SortBottomSheetResult(
                sortOptionItem = sharesState.selectedSortConfiguration.sortOption,
                sortDirection = sharesState.selectedSortConfiguration.sortDirection
            ),
            onSortOptionSelected = { result ->
                result?.let {
                    setSortOrder(
                        NodeSortConfiguration(
                            sortOption = it.sortOptionItem,
                            sortDirection = it.sortDirection
                        )
                    )
                    showSortBottomSheet = false
                }
            },
            onDismissRequest = {
                showSortBottomSheet = false
            }
        )
    }
}

internal const val CONTACT_SHARED_FOLDERS_APP_BAR_TAG = "contact_shared_folders_screen:app_bar"
internal const val CONTACT_SHARED_FOLDERS_SELECTION_MODE_APP_BAR_TAG =
    "contact_shared_folders_screen:selection_mode_app_bar"
internal const val CONTACT_SHARED_FOLDERS_SELECTION_MODE_BOTTOM_BAR_TAG =
    "contact_shared_folders_screen:selection_mode_bottom_bar"
internal const val CONTACT_SHARED_FOLDERS_CONTENT_TAG = "contact_shared_folders_screen:content"
internal const val CONTACT_SHARED_FOLDERS_TRANSFERS_WIDGET_TAG =
    "contact_shared_folders_screen:transfers_widget"
