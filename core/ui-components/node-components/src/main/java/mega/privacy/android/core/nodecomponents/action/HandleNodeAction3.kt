package mega.privacy.android.core.nodecomponents.action

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.privacy.android.core.nodecomponents.mapper.NodeSourceTypeToViewTypeMapper
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent

/**
 * Handle node action click for m3
 */
@Composable
fun HandleNodeAction3(
    typedFileNode: TypedFileNode,
    snackBarHostState: SnackbarHostState?,
    onActionHandled: () -> Unit,
    coroutineScope: CoroutineScope,
    nodeSourceType: NodeSourceType,
    onDownloadEvent: (TransferTriggerEvent) -> Unit = {},
    sortOrder: SortOrder = SortOrder.ORDER_NONE,
) {
    val nodeSourceTypeToViewTypeMapper = remember {
        NodeSourceTypeToViewTypeMapper()
    }
    BaseHandleNodeAction(
        typedFileNode = typedFileNode,
        showSnackbar = { message ->
            coroutineScope.launch {
                snackBarHostState?.showAutoDurationSnackbar(message)
            }
        },
        onActionHandled = onActionHandled,
        coroutineScope = coroutineScope,
        nodeSourceType = nodeSourceTypeToViewTypeMapper(nodeSourceType),
        onDownloadEvent = onDownloadEvent,
        sortOrder = sortOrder
    )
}