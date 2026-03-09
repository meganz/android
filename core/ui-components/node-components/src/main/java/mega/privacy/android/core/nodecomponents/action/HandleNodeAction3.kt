package mega.privacy.android.core.nodecomponents.action

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.privacy.android.core.nodecomponents.mapper.FileNodeContentToNavKeyMapper
import mega.privacy.android.core.nodecomponents.mapper.NodeSourceTypeToViewTypeMapper
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.FileNodeContent
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent

/**
 * Handle node action click for m3
 */
@Composable
fun HandleNodeAction3(
    typedFileNode: TypedFileNode,
    snackBarHostState: SnackbarHostState?,
    onNavigate: (NavKey) -> Unit,
    onActionHandled: () -> Unit,
    coroutineScope: CoroutineScope,
    nodeSourceData: NodeSourceData,
    onDownloadEvent: (TransferTriggerEvent) -> Unit = {},
    sortOrder: SortOrder = SortOrder.ORDER_NONE,
) {
    val fileNodeContentToNavKeyMapper = remember {
        FileNodeContentToNavKeyMapper(NodeSourceTypeToViewTypeMapper())
    }
    BaseHandleNodeAction(
        typedFileNode = typedFileNode,
        isLinkNode = nodeSourceData.isPublicLinkSource(),
        showSnackbar = { message ->
            coroutineScope.launch {
                snackBarHostState?.showAutoDurationSnackbar(message)
            }
        },
        onActionHandled = onActionHandled,
        onOpenFileContent = { content: FileNodeContent, isTextEditorComposeEnabled: Boolean, isPDFViewerEnabled: Boolean ->
            fileNodeContentToNavKeyMapper(
                content = content,
                fileNode = typedFileNode,
                nodeSourceData = nodeSourceData,
                sortOrder = sortOrder,
                isTextEditorComposeEnabled = isTextEditorComposeEnabled,
                isPDFViewerEnabled = isPDFViewerEnabled,
            )?.let { navKey ->
                onNavigate(navKey)
            }
        },
        onDownloadEvent = onDownloadEvent,
    )
}

