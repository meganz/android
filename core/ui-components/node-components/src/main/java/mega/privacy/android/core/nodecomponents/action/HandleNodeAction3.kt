package mega.privacy.android.core.nodecomponents.action

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.privacy.android.core.nodecomponents.mapper.NodeSourceTypeToViewTypeMapper
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.LegacyImageViewerNavKey
import mega.privacy.android.navigation.destination.LegacyMediaPlayerNavKey
import mega.privacy.android.navigation.destination.LegacyPdfViewerNavKey
import mega.privacy.android.navigation.destination.LegacyTextEditorNavKey
import mega.privacy.android.navigation.destination.LegacyZipBrowserNavKey

/**
 * Handle node action click for m3
 */
@Composable
fun HandleNodeAction3(
    typedFileNode: TypedFileNode,
    snackBarHostState: SnackbarHostState?,
    navigationHandler: NavigationHandler,
    onActionHandled: () -> Unit,
    coroutineScope: CoroutineScope,
    nodeSourceType: NodeSourceType,
    onDownloadEvent: (TransferTriggerEvent) -> Unit = {},
    sortOrder: SortOrder = SortOrder.ORDER_NONE,
) {
    val nodeSourceTypeToViewTypeMapper = remember {
        NodeSourceTypeToViewTypeMapper()
    }
    val viewType = nodeSourceTypeToViewTypeMapper(nodeSourceType)
    BaseHandleNodeAction(
        typedFileNode = typedFileNode,
        showSnackbar = { message ->
            coroutineScope.launch {
                snackBarHostState?.showAutoDurationSnackbar(message)
            }
        },
        onActionHandled = onActionHandled,
        onOpenPdf = { nodeContentUri: NodeContentUri ->
            navigationHandler.navigate(
                LegacyPdfViewerNavKey(
                    nodeHandle = typedFileNode.id.longValue,
                    nodeContentUri = nodeContentUri,
                    nodeSourceType = viewType,
                    mimeType = typedFileNode.type.mimeType
                )
            )
        },
        onOpenImageViewer = {
            navigationHandler.navigate(
                LegacyImageViewerNavKey(
                    nodeHandle = typedFileNode.id.longValue,
                    parentNodeHandle = typedFileNode.parentId.longValue,
                    nodeSourceType = viewType
                )
            )
        },
        onOpenTextEditor = { mode: TextEditorMode ->
            navigationHandler.navigate(
                LegacyTextEditorNavKey(
                    nodeHandle = typedFileNode.id.longValue,
                    mode = mode.value,
                    nodeSourceType = viewType
                )
            )
        },
        onOpenMediaPlayer = { nodeContentUri: NodeContentUri ->
            navigationHandler.navigate(
                LegacyMediaPlayerNavKey(
                    nodeHandle = typedFileNode.id.longValue,
                    nodeContentUri = nodeContentUri,
                    nodeSourceType = viewType,
                    sortOrder = sortOrder,
                    isFolderLink = false,
                    fileName = typedFileNode.name,
                    parentHandle = typedFileNode.parentId.longValue,
                    fileHandle = typedFileNode.id.longValue,
                    fileTypeInfo = typedFileNode.type,
                )
            )
        },
        onOpenZipBrowser = { zipFilePath: String, _, _: () -> Unit ->
            navigationHandler.navigate(
                LegacyZipBrowserNavKey(
                    zipFilePath = zipFilePath
                )
            )
        },
        coroutineScope = coroutineScope,
        onDownloadEvent = onDownloadEvent,
    )
}