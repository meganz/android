package mega.privacy.android.core.nodecomponents.sheet.options

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import de.palm.composestateevents.EventEffect
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.core.nodecomponents.action.NodeActionHandler
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.action.rememberNodeActionHandler
import mega.privacy.android.core.nodecomponents.list.NodeListViewItem
import mega.privacy.android.core.nodecomponents.mapper.NodeBottomSheetState
import mega.privacy.android.core.nodecomponents.model.NodeActionModeMenuItem
import mega.privacy.android.core.nodecomponents.model.text
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeOptionsBottomSheetRoute(
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

    LaunchedEffect(Unit) {
        keyboardController?.hide()
        viewModel.getBottomSheetOptions(nodeId, nodeSourceType)
    }

    EventEffect(
        event = nodeOptionActionState.downloadEvent,
        onConsumed = nodeOptionsActionViewModel::markDownloadEventConsumed,
        action = onTransfer
    )

    NodeOptionsBottomSheetContent(
        uiState = uiState,
        actionHandler = actionHandler,
        onDismiss = onDismiss,
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
    actionHandler: NodeActionHandler,
    onDismiss: () -> Unit,
    onConsumeErrorState: () -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()
    // Todo: We will remove this, and replace it with NavigationHandler
    val navHostController = rememberNavController()

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
                onDismiss,
                actionHandler,
                navHostController,
                coroutineScope
            )
        }
    }
}