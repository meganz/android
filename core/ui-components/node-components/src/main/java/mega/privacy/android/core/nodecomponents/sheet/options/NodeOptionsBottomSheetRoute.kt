package mega.privacy.android.core.nodecomponents.sheet.options

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mega.android.core.ui.model.SnackbarAttributes
import mega.android.core.ui.modifiers.shimmerEffect
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionHandler
import mega.privacy.android.core.nodecomponents.action.rememberSingleNodeActionHandler
import mega.privacy.android.core.nodecomponents.dialog.sharefolder.ShareFolderAccessDialogNavKey
import mega.privacy.android.core.nodecomponents.dialog.sharefolder.ShareFolderDialogNavKey
import mega.privacy.android.core.nodecomponents.dialog.sharefolder.ShareFolderDialogResult
import mega.privacy.android.core.nodecomponents.list.NodeListViewItem
import mega.privacy.android.core.nodecomponents.mapper.NodeBottomSheetState
import mega.privacy.android.core.nodecomponents.mapper.NodeHandlesToJsonMapper
import mega.privacy.android.core.nodecomponents.model.BottomSheetClickHandler
import mega.privacy.android.core.nodecomponents.model.NodeActionModeMenuItem
import mega.privacy.android.core.nodecomponents.model.text
import mega.privacy.android.core.nodecomponents.sheet.options.NodeOptionsBottomSheetResult.RestoreSuccess
import mega.privacy.android.domain.entity.node.AddVideoToPlaylistResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionsResult
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.isSharedSource
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.megaActivityResultContract
import timber.log.Timber

/**
 * Delay before showing actual content to allow bottom sheet animation to complete.
 * This prevents visual flickering when transitioning from skeleton to content.
 */
private const val SHEET_READY_DELAY_MS = 250L

/**
 * Number of skeleton menu items to show during loading state.
 */
private const val SKELETON_MENU_ITEM_COUNT = 6

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NodeOptionsBottomSheetRoute(
    navigationHandler: NavigationHandler,
    onDismiss: () -> Unit,
    nodeId: Long,
    nodeSourceType: NodeSourceType,
    partiallyExpand: Boolean,
    onTransfer: (TransferTriggerEvent) -> Unit,
    onNavigate: (NavKey) -> Unit = {},
    onRename: (NodeId) -> Unit = {},
    onCollisionResult: (NodeNameCollisionsResult) -> Unit = {},
    onRestoreSuccess: (RestoreSuccess.RestoreData) -> Unit = {},
    onAddVideoToPlaylistResult: (AddVideoToPlaylistResult) -> Unit = {},
    nodeOptionsActionViewModel: NodeOptionsActionViewModel = hiltViewModel(),
    actionHandler: SingleNodeActionHandler = rememberSingleNodeActionHandler(
        navigationHandler = navigationHandler,
        viewModel = nodeOptionsActionViewModel
    ),
    viewModel: NodeOptionsBottomSheetViewModel = hiltViewModel(),
    shareFolderDialogResult: ShareFolderDialogResult? = null,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val nodeOptionActionState by nodeOptionsActionViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val megaActivityResultContract = remember { context.megaActivityResultContract }
    val shareFolderLauncher = rememberLauncherForActivityResult(
        contract = megaActivityResultContract.shareFolderActivityResultContract
    ) { result ->
        result?.let { (contactIds, nodeHandles) ->
            nodeOptionsActionViewModel.contactSelectedForShareFolder(contactIds, nodeHandles)
        }
    }
    val nodeHandlesToJsonMapper = remember { NodeHandlesToJsonMapper() }

    LaunchedEffect(Unit) {
        keyboardController?.hide()
        viewModel.getBottomSheetOptions(nodeId, nodeSourceType)
    }

    LaunchedEffect(shareFolderDialogResult) {
        shareFolderDialogResult?.let {
            val handles = it.nodes.map { node -> node.id.longValue }.toLongArray()
            shareFolderLauncher.launch(handles)
        }
    }

    // Event handlers
    EventEffect(
        event = nodeOptionActionState.downloadEvent,
        onConsumed = nodeOptionsActionViewModel::markDownloadEventConsumed,
        action = onTransfer
    )

    EventEffect(
        event = nodeOptionActionState.shareFolderDialogEvent,
        onConsumed = nodeOptionsActionViewModel::resetShareFolderDialogEvent,
        action = { handles ->
            onNavigate(ShareFolderDialogNavKey(nodeHandlesToJsonMapper(handles)))
        }
    )

    EventEffect(
        event = nodeOptionActionState.shareFolderEvent,
        onConsumed = nodeOptionsActionViewModel::resetShareFolderEvent,
        action = { handles -> shareFolderLauncher.launch(handles.toLongArray()) }
    )

    EventEffect(
        event = nodeOptionActionState.contactsData,
        onConsumed = nodeOptionsActionViewModel::markShareFolderAccessDialogShown,
        action = { (contactData, isFromBackups, nodeHandles) ->
            onNavigate(
                ShareFolderAccessDialogNavKey(
                    nodes = nodeHandles,
                    contacts = contactData.joinToString(separator = ","),
                    isFromBackups = isFromBackups
                )
            )
        },
    )

    EventEffect(
        event = nodeOptionActionState.navigationEvent,
        onConsumed = nodeOptionsActionViewModel::resetNavigationEvent,
        action = onNavigate
    )

    EventEffect(
        event = nodeOptionActionState.dismissEvent,
        onConsumed = nodeOptionsActionViewModel::resetDismiss,
        action = onDismiss
    )

    EventEffect(
        event = nodeOptionActionState.renameNodeRequestEvent,
        onConsumed = nodeOptionsActionViewModel::resetRenameNodeRequest,
        action = onRename
    )

    EventEffect(
        event = nodeOptionActionState.nodeNameCollisionsResult,
        onConsumed = nodeOptionsActionViewModel::markHandleNodeNameCollisionResult,
        action = onCollisionResult
    )

    EventEffect(
        event = nodeOptionActionState.restoreSuccessEvent,
        onConsumed = nodeOptionsActionViewModel::resetRestoreSuccessEvent,
        action = onRestoreSuccess
    )

    EventEffect(
        event = nodeOptionActionState.addVideoToPlaylistResultEvent,
        onConsumed = nodeOptionsActionViewModel::resetAddVideoToPlaylistResultEvent,
        action = onAddVideoToPlaylistResult
    )

    NodeOptionsBottomSheetContent(
        uiState = uiState,
        navigationHandler = navigationHandler,
        actionHandler = actionHandler,
        nodeSourceType = nodeSourceType,
        partiallyExpand = partiallyExpand,
        onDismiss = onDismiss,
        showSnackbar = viewModel::showSnackbar,
        onConsumeErrorState = viewModel::onConsumeErrorState,
    )
}

/**
 * Node options bottom sheet content.
 *
 * Displays a loading skeleton initially to ensure proper partial expansion behavior,
 * then transitions to the actual content after a brief delay.
 *
 * @param uiState The current state of the bottom sheet
 * @param navigationHandler Handler for navigation events
 * @param actionHandler Handler for node actions
 * @param nodeSourceType The source type of the node
 * @param partiallyExpand Whether the sheet should support partial expansion
 * @param onDismiss Callback when the sheet should be dismissed
 * @param showSnackbar Callback to show snackbar messages
 * @param onConsumeErrorState Callback to consume error state
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun NodeOptionsBottomSheetContent(
    uiState: NodeBottomSheetState,
    navigationHandler: NavigationHandler,
    actionHandler: SingleNodeActionHandler,
    nodeSourceType: NodeSourceType,
    partiallyExpand: Boolean,
    onDismiss: () -> Unit,
    showSnackbar: suspend (SnackbarAttributes) -> Unit,
    onConsumeErrorState: () -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }

    var isSheetReady by remember { mutableStateOf(false) }
    val isContentReady = isSheetReady && uiState.node != null

    LaunchedEffect(Unit) {
        // Delay should only be applied on first load
        if (uiState.actions.isEmpty()) {
            delay(SHEET_READY_DELAY_MS)
        }

        isSheetReady = true
    }

    EventEffect(
        event = uiState.error,
        onConsumed = onConsumeErrorState,
        action = {
            Timber.e(it)
            onDismiss()
        },
    )

    if (isContentReady) {
        NodeOptionsHeader(
            uiState = uiState,
            nodeSourceType = nodeSourceType,
            isScrolled = isScrolled,
        )

        if (uiState.actions.isNotEmpty()) {
            NodeOptionsActionList(
                actions = uiState.actions,
                listState = listState,
                actionHandler = actionHandler,
                navigationHandler = navigationHandler,
                coroutineScope = coroutineScope,
                context = context,
                showSnackbar = showSnackbar,
            )
        }
    } else {
        NodeOptionsLoadingSkeleton(partiallyExpand = partiallyExpand)
    }
}

/**
 * Header section showing node info.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun NodeOptionsHeader(
    uiState: NodeBottomSheetState,
    nodeSourceType: NodeSourceType,
    isScrolled: Boolean,
) {
    val node = uiState.node ?: return

    NodeListViewItem(
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        title = node.title.text,
        titleColor = if (node.isTakenDown) TextColor.Error else TextColor.Primary,
        titleTextStyle = AppTheme.typography.titleMedium,
        titleMaxLines = 1,
        subtitle = node.subtitle.text(),
        showVersion = node.hasVersion,
        icon = node.iconRes,
        thumbnailData = node.thumbnailData,
        accessPermissionIcon = node.accessPermissionIcon,
        showBlurEffect = !nodeSourceType.isSharedSource() && node.showBlurEffect,
        isSensitive = !nodeSourceType.isSharedSource() && node.isSensitive,
        onItemClicked = {},
        enableClick = false,
    )

    if (isScrolled) {
        HorizontalDivider(
            modifier = Modifier.testTag(NODE_OPTIONS_HEADER_DIVIDER_TEST_TAG),
            color = DSTokens.colors.border.strong,
            thickness = 0.3.dp
        )
    }
}

/**
 * Action list section showing menu items.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun NodeOptionsActionList(
    actions: List<List<NodeActionModeMenuItem>>,
    listState: LazyListState,
    actionHandler: SingleNodeActionHandler,
    navigationHandler: NavigationHandler,
    coroutineScope: CoroutineScope,
    context: Context,
    showSnackbar: suspend (SnackbarAttributes) -> Unit,
) {
    val clickHandler = remember(actionHandler, navigationHandler, coroutineScope, context) {
        BottomSheetClickHandler(
            actionHandler = actionHandler,
            navigationHandler = navigationHandler,
            coroutineScope = coroutineScope,
            context = context,
            snackbarHandler = { attributes ->
                coroutineScope.launch { showSnackbar(attributes) }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .semantics { testTagsAsResourceId = true }
            .testTag(NODE_OPTIONS_LAZY_COLUMN_TEST_TAG),
        state = listState
    ) {
        actions.forEachIndexed { index, actionGroup ->
            items(actionGroup) { item ->
                item.control(clickHandler)
            }

            if (index < actions.size - 1) {
                item {
                    HorizontalDivider(
                        modifier = Modifier.testTag(NODE_OPTIONS_GROUP_DIVIDER_TEST_TAG),
                        color = DSTokens.colors.border.subtle
                    )
                }
            }
        }
    }
}

/**
 * Loading skeleton for the node options bottom sheet.
 *
 * Shows shimmer placeholders that mimic the final content layout,
 * providing smooth visual transition when content loads.
 *
 * @param partiallyExpand When true, fills the available space to ensure sheet height
 *        exceeds 50% of screen height, enabling proper partial expansion behavior.
 *        When false, shows only the header skeleton.
 */
@Composable
private fun NodeOptionsLoadingSkeleton(partiallyExpand: Boolean) {
    Column(
        modifier = if (partiallyExpand) Modifier.fillMaxSize() else Modifier
    ) {
        // Header skeleton
        HeaderSkeleton()

        // Menu items skeleton (only for partial expansion)
        if (partiallyExpand) {
            repeat(SKELETON_MENU_ITEM_COUNT) {
                MenuItemSkeleton()
            }
        }
    }
}

/**
 * Skeleton for the header row (mimics NodeListViewItem).
 */
@Composable
private fun HeaderSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(
            modifier = Modifier
                .size(32.dp)
                .shimmerEffect(RoundedCornerShape(8.dp))
        )
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        ) {
            Spacer(
                modifier = Modifier
                    .height(18.dp)
                    .fillMaxWidth(0.6f)
                    .shimmerEffect()
            )
            Spacer(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .height(12.dp)
                    .fillMaxWidth(0.4f)
                    .shimmerEffect()
            )
        }
    }
}

/**
 * Skeleton for a single menu item row.
 */
@Composable
private fun MenuItemSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(
            modifier = Modifier
                .size(24.dp)
                .shimmerEffect(RoundedCornerShape(4.dp))
        )
        Spacer(
            modifier = Modifier
                .padding(start = 32.dp)
                .height(16.dp)
                .width(120.dp)
                .shimmerEffect()
        )
    }
}

const val NODE_OPTIONS_HEADER_DIVIDER_TEST_TAG = "node_options_bottom_sheet:header_divider"
const val NODE_OPTIONS_LAZY_COLUMN_TEST_TAG = "node_options_bottom_sheet:lazy_column"
const val NODE_OPTIONS_GROUP_DIVIDER_TEST_TAG = "node_options_bottom_sheet:group_divider"
