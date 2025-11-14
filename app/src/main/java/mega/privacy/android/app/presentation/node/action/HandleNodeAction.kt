package mega.privacy.android.app.presentation.node.action

import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.node.NodeActionsViewModel
import mega.privacy.android.app.presentation.snackbar.SnackbarHostStateWrapper
import mega.privacy.android.app.presentation.snackbar.showAutoDurationSnackbar
import mega.privacy.android.core.nodecomponents.action.BaseHandleNodeAction
import mega.privacy.android.core.nodecomponents.action.openOtherFile
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.FileNodeContent
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.navigation.megaNavigator
import mega.privacy.android.shared.resources.R
import java.io.File

/**
 * Handle node action click
 *
 * @param typedFileNode [TypedFileNode]
 * @param nodeSourceType from where item click is performed
 * @param onDownloadEvent callback for download event
 * @param sortOrder [SortOrder]
 * @param snackBarHostState [SnackbarHostState]
 * @param onActionHandled callback after file clicked
 */
@Composable
fun HandleNodeAction(
    typedFileNode: TypedFileNode,
    snackBarHostState: SnackbarHostState,
    onActionHandled: () -> Unit,
    coroutineScope: CoroutineScope,
    nodeSourceType: Int? = null,
    onDownloadEvent: (TransferTriggerEvent) -> Unit = {},
    sortOrder: SortOrder = SortOrder.ORDER_NONE,
) {
    val snackbarHostStateWrapper = remember {
        SnackbarHostStateWrapper(snackBarHostStateM2 = snackBarHostState)
    }
    val context = LocalContext.current
    val megaNavigator = remember { context.megaNavigator }
    BaseHandleNodeAction(
        typedFileNode = typedFileNode,
        showSnackbar = { message ->
            coroutineScope.launch {
                snackbarHostStateWrapper.showAutoDurationSnackbar(message)
            }
        },
        onActionHandled = onActionHandled,
        onOpenFileContent = { content ->
            when (content) {
                is FileNodeContent.Pdf -> {
                    megaNavigator.openPdfActivity(
                        context = context,
                        content = content.uri,
                        type = nodeSourceType,
                        currentFileNode = typedFileNode
                    )
                }

                FileNodeContent.ImageForNode -> {
                    megaNavigator.openImageViewerActivity(
                        context = context,
                        currentFileNode = typedFileNode,
                        nodeSourceType = nodeSourceType
                    )
                }

                FileNodeContent.TextContent -> {
                    megaNavigator.openTextEditorActivity(
                        context = context,
                        currentNodeId = typedFileNode.id,
                        nodeSourceType = nodeSourceType,
                        mode = TextEditorMode.View
                    )
                }

                is FileNodeContent.AudioOrVideo -> {
                    megaNavigator.openMediaPlayerActivityByFileNode(
                        context = context,
                        contentUri = content.uri,
                        fileNode = typedFileNode,
                        viewType = nodeSourceType,
                        sortOrder = sortOrder,
                        isFolderLink = false
                    )
                }

                is FileNodeContent.LocalZipFile -> {
                    megaNavigator.openZipBrowserActivity(
                        context = context,
                        zipFilePath = content.localFile.absolutePath,
                        nodeHandle = typedFileNode.id.longValue,
                        onError = {
                            coroutineScope.launch {
                                snackbarHostStateWrapper.showAutoDurationSnackbar(
                                    context.getString(R.string.message_zip_format_error)
                                )
                            }
                        }
                    )
                }

                is FileNodeContent.ImageForChat, is FileNodeContent.Other, is FileNodeContent.UrlContent -> {
                    // no-opt here
                }
            }
        },
        onDownloadEvent = onDownloadEvent,
    )
}


/**
 * Handle node action click
 */
@Composable
fun HandleFileAction(
    file: File,
    isOpenWith: Boolean,
    snackBarHostState: SnackbarHostStateWrapper?,
    onActionHandled: () -> Unit,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
) {
    val nodeActionsViewModel: NodeActionsViewModel = hiltViewModel()
    val context = LocalContext.current
    val megaNavigator = remember { context.megaNavigator }

    LaunchedEffect(file) {
        openOtherFile(
            onOpenZipBrowser = { zipFilePath, nodeHandle, onError ->
                megaNavigator.openZipBrowserActivity(
                    context = context,
                    zipFilePath = zipFilePath,
                    nodeHandle = nodeHandle,
                    onError = onError
                )
            },
            file = file,
            typedFileNode = null,
            isOpenWith = isOpenWith,
            fileTypeInfo = nodeActionsViewModel.getTypeInfo(file),
            applyNodeContentUri = nodeActionsViewModel::applyNodeContentUri,
            showSnackbar = { message ->
                coroutineScope.launch {
                    snackBarHostState?.showAutoDurationSnackbar(message)
                }
            },
            context = context,
        )

        onActionHandled()
    }
}
