package mega.privacy.android.core.nodecomponents.sheet.options

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.launch
import mega.android.core.ui.model.SnackbarAttributes
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.core.nodecomponents.action.NodeActionHandler
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.action.rememberNodeActionHandler
import mega.privacy.android.core.nodecomponents.dialog.sharefolder.ShareFolderDialogM3
import mega.privacy.android.core.nodecomponents.list.NodeListViewItem
import mega.privacy.android.core.nodecomponents.mapper.NodeBottomSheetState
import mega.privacy.android.core.nodecomponents.model.BottomSheetClickHandler
import mega.privacy.android.core.nodecomponents.model.NodeActionModeMenuItem
import mega.privacy.android.core.nodecomponents.model.text
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.megaActivityResultContract
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeOptionsBottomSheetRoute(
    navigationHandler: NavigationHandler,
    onDismiss: () -> Unit,
    nodeId: Long,
    nodeSourceType: NodeSourceType,
    onTransfer: (TransferTriggerEvent) -> Unit,
    nodeOptionsActionViewModel: NodeOptionsActionViewModel = hiltViewModel(),
    actionHandler: NodeActionHandler = rememberNodeActionHandler(nodeOptionsActionViewModel),
    viewModel: NodeOptionsBottomSheetViewModel = hiltViewModel(),
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val nodeOptionActionState by nodeOptionsActionViewModel.uiState.collectAsStateWithLifecycle()
    var shareNodeHandles by remember { mutableStateOf<List<Long>>(emptyList()) }
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

    LaunchedEffect(Unit) {
        keyboardController?.hide()
        viewModel.getBottomSheetOptions(nodeId, nodeSourceType)
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
            shareNodeHandles = handles
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
            onDismiss()
            // Todo: NavigationHandler implement share folder access dialog. M3 version of this dialog is not available yet
//            val contactList =
//                contactData.joinToString(separator = contactArraySeparator)
//            navHostController.navigate(
//                shareFolderAccessDialog.plus("/${contactList}")
//                    .plus("/${isFromBackups}")
//                    .plus("/${nodeHandles}")
//            )
        },
    )

    NodeOptionsBottomSheetContent(
        uiState = uiState,
        navigationHandler = navigationHandler,
        actionHandler = actionHandler,
        onDismiss = onDismiss,
        showSnackbar = viewModel::showSnackbar,
        onConsumeErrorState = viewModel::onConsumeErrorState,
    )

    if (shareNodeHandles.isNotEmpty()) {
        // We cannot provide navigation to this dialog, because it requires injecting shareFolderLauncher
        // to the menu item. Instead the menu item will call the view model to trigger StateEvent
        // to show this dialog, and the onConfirm callback will use the launcher to start the activity for result
        ShareFolderDialogM3(
            nodeIds = shareNodeHandles.map { NodeId(it) },
            onDismiss = {
                shareNodeHandles = emptyList()
                onDismiss()
            },
            onConfirm = { nodes ->
                val handles = nodes.map { it.id.longValue }.toLongArray()
                shareFolderLauncher.launch(handles)
            }
        )
    }
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
    actionHandler: NodeActionHandler,
    onDismiss: () -> Unit,
    showSnackbar: suspend (SnackbarAttributes) -> Unit,
    onConsumeErrorState: () -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

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
            subtitle = uiState.node.subtitle.text(),
            showVersion = uiState.node.hasVersion,
            icon = uiState.node.iconRes,
            thumbnailData = uiState.node.thumbnailData,
            accessPermissionIcon = uiState.node.accessPermissionIcon,
            onItemClicked = {}
        )
    }

    LazyColumn(modifier = Modifier.semantics { testTagsAsResourceId = true }) {
        items(uiState.actions) { item: NodeActionModeMenuItem ->
            item.control(
                BottomSheetClickHandler(
                    onDismiss = onDismiss,
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
    }
}