package mega.privacy.android.core.nodecomponents.sheet.options

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import mega.android.core.ui.model.SnackbarAttributes
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NodeOptionsBottomSheetRoute(
    navigationHandler: NavigationHandler,
    onDismiss: () -> Unit,
    nodeId: Long,
    nodeSourceType: NodeSourceType,
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
            nodeOptionsActionViewModel.contactSelectedForShareFolder(
                contactIds,
                nodeHandles
            )
        }
    }
    val nodeHandlesToJsonMapper = remember { NodeHandlesToJsonMapper() }

    LaunchedEffect(Unit) {
        keyboardController?.hide()
        viewModel.getBottomSheetOptions(nodeId, nodeSourceType)
    }

    LaunchedEffect(shareFolderDialogResult) {
        if (shareFolderDialogResult != null) {
            val handles = shareFolderDialogResult.nodes.map { it.id.longValue }.toLongArray()
            shareFolderLauncher.launch(handles)
        }
    }

    EventEffect(
        event = nodeOptionActionState.downloadEvent,
        onConsumed = nodeOptionsActionViewModel::markDownloadEventConsumed,
        action = onTransfer
    )

    EventEffect(
        event = nodeOptionActionState.shareFolderDialogEvent,
        onConsumed = nodeOptionsActionViewModel::resetShareFolderDialogEvent,
        action = { handles ->
            val nodes = nodeHandlesToJsonMapper(handles)
            onNavigate(ShareFolderDialogNavKey(nodes))
        }
    )

    EventEffect(
        event = nodeOptionActionState.shareFolderEvent,
        onConsumed = nodeOptionsActionViewModel::resetShareFolderEvent,
        action = { handles ->
            shareFolderLauncher.launch(handles.toLongArray())
        }
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
        onDismiss = onDismiss,
        showSnackbar = viewModel::showSnackbar,
        onConsumeErrorState = viewModel::onConsumeErrorState,
    )
}

/**
 * Node options bottom sheet content for the node-components module
 * Uses NodeActionLauncher interface to launch activities from app module
 * while handling callbacks through NodeOptionsActionViewModel
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun NodeOptionsBottomSheetContent(
    uiState: NodeBottomSheetState,
    navigationHandler: NavigationHandler,
    actionHandler: SingleNodeActionHandler,
    nodeSourceType: NodeSourceType,
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

    EventEffect(
        event = uiState.error,
        onConsumed = onConsumeErrorState,
        action = {
            Timber.e(it)
            onDismiss()
        },
    )

    if (uiState.node != null) {
        NodeListViewItem(
            modifier = Modifier.semantics { testTagsAsResourceId = true },
            title = uiState.node.name,
            titleColor = if (uiState.node.isTakenDown) TextColor.Error else TextColor.Primary,
            titleTextStyle = AppTheme.typography.titleMedium,
            titleMaxLines = 1,
            subtitle = uiState.node.subtitle.text(),
            showVersion = uiState.node.hasVersion,
            icon = uiState.node.iconRes,
            thumbnailData = uiState.node.thumbnailData,
            accessPermissionIcon = uiState.node.accessPermissionIcon,
            // Shared items (incoming shares, outgoing shares, links) should not show blur effect
            // or sensitive flags as they are not owned by the current user
            showBlurEffect = !nodeSourceType.isSharedSource() && uiState.node.showBlurEffect,
            isSensitive = !nodeSourceType.isSharedSource() && uiState.node.isSensitive,
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

    LazyColumn(
        modifier = Modifier
            .semantics { testTagsAsResourceId = true }
            .testTag(NODE_OPTIONS_LAZY_COLUMN_TEST_TAG),
        state = listState
    ) {
        uiState.actions.forEachIndexed { index, actions ->
            items(actions) { item ->
                item.control(
                    BottomSheetClickHandler(
                        actionHandler = actionHandler,
                        navigationHandler = navigationHandler,
                        coroutineScope = coroutineScope,
                        context = context,
                        snackbarHandler = {
                            coroutineScope.launch {
                                showSnackbar(it)
                            }
                        }
                    )
                )
            }

            if (index < uiState.actions.size - 1) {
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

const val NODE_OPTIONS_HEADER_DIVIDER_TEST_TAG = "node_options_bottom_sheet:header_divider"
const val NODE_OPTIONS_LAZY_COLUMN_TEST_TAG = "node_options_bottom_sheet:lazy_column"
const val NODE_OPTIONS_GROUP_DIVIDER_TEST_TAG = "node_options_bottom_sheet:group_divider"
