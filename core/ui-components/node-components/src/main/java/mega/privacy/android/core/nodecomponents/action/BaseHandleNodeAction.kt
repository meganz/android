package mega.privacy.android.core.nodecomponents.action

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.ZipFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNodeContent
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.navigation.megaNavigator
import mega.privacy.android.shared.resources.R as sharedR
import timber.log.Timber
import java.io.File

/**
 * Handle node action click
 *
 * @param typedFileNode [TypedFileNode]
 * @param nodeSourceType from where item click is performed
 * @param sortOrder [SortOrder]
 * @param showSnackbar callback to show snackbar messages
 * @param onActionHandled callback after file clicked
 * @param onDownloadEvent callback for download event
 */
@Composable
fun BaseHandleNodeAction(
    typedFileNode: TypedFileNode,
    showSnackbar: (String) -> Unit,
    onActionHandled: () -> Unit,
    nodeSourceType: Int? = null,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    sortOrder: SortOrder = SortOrder.ORDER_NONE,
    onDownloadEvent: (TransferTriggerEvent) -> Unit = {},
) {
    val nodeActionsViewModel: NodeActionHandlerViewModel = hiltViewModel()
    val context = LocalContext.current
    val megaNavigator = remember { context.megaNavigator }

    LaunchedEffect(key1 = typedFileNode) {
        runCatching {
            nodeActionsViewModel.handleFileNodeClicked(typedFileNode)
        }.onSuccess { content ->
            when (content) {
                is FileNodeContent.Pdf -> megaNavigator.openPdfActivity(
                    context = context,
                    content = content.uri,
                    type = nodeSourceType,
                    currentFileNode = typedFileNode
                )

                is FileNodeContent.ImageForNode -> {
                    megaNavigator.openImageViewerActivity(
                        context = context,
                        currentFileNode = typedFileNode,
                        nodeSourceType = nodeSourceType,
                    )
                }

                is FileNodeContent.TextContent -> megaNavigator.openTextEditorActivity(
                    context = context,
                    currentNodeId = typedFileNode.id,
                    nodeSourceType = nodeSourceType,
                    mode = TextEditorMode.View
                )

                is FileNodeContent.AudioOrVideo -> {
                    openVideoOrAudioFile(
                        megaNavigator = megaNavigator,
                        context = context,
                        content = content.uri,
                        fileNode = typedFileNode,
                        showSnackbar = showSnackbar,
                        sortOrder = sortOrder,
                        viewType = nodeSourceType,
                        coroutineScope = coroutineScope,
                    )
                }

                is FileNodeContent.UrlContent -> {
                    openUrlFile(
                        context = context,
                        content = content,
                        showSnackbar = showSnackbar
                    )
                }

                is FileNodeContent.Other -> {
                    content.localFile?.let {
                        openOtherFile(
                            megaNavigator = megaNavigator,
                            file = it,
                            typedFileNode = typedFileNode,
                            isOpenWith = false,
                            fileTypeInfo = typedFileNode.type,
                            applyNodeContentUri = nodeActionsViewModel::applyNodeContentUri,
                            showSnackbar = showSnackbar,
                            context = context,
                        )
                    } ?: run {
                        onDownloadEvent(
                            TransferTriggerEvent.StartDownloadForPreview(
                                node = typedFileNode,
                                isOpenWith = false
                            )
                        )
                    }
                }

                else -> {

                }
            }
            onActionHandled()
        }.onFailure {
            Timber.e(it)
        }
    }
}


private fun openVideoOrAudioFile(
    megaNavigator: MegaNavigator,
    context: Context,
    fileNode: TypedFileNode,
    content: NodeContentUri,
    showSnackbar: (String) -> Unit,
    sortOrder: SortOrder,
    viewType: Int?,
    coroutineScope: CoroutineScope,
) {
    coroutineScope.launch {
        runCatching {
            megaNavigator.openMediaPlayerActivityByFileNode(
                context = context,
                contentUri = content,
                fileNode = fileNode,
                sortOrder = sortOrder,
                viewType = viewType,
                isFolderLink = false,
            )
        }.onFailure {
            showSnackbar(context.getString(sharedR.string.intent_not_available))
        }
    }
}

private fun openUrlFile(
    context: Context,
    content: FileNodeContent.UrlContent,
    showSnackbar: (String) -> Unit,
) {
    content.path?.let {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = it.toUri()
        }
        safeLaunchActivity(
            context = context,
            intent = intent,
            showSnackbar = showSnackbar
        )
    } ?: run {
        showSnackbar(context.getString(sharedR.string.intent_not_available))
    }
}

private fun safeLaunchActivity(
    context: Context,
    intent: Intent,
    showSnackbar: (String) -> Unit,
) {
    runCatching {
        context.startActivity(intent)
    }.onFailure {
        Timber.e(it)
        showSnackbar(context.getString(sharedR.string.intent_not_available))
    }
}

fun openOtherFile(
    megaNavigator: MegaNavigator,
    file: File,
    typedFileNode: TypedFileNode?,
    isOpenWith: Boolean,
    fileTypeInfo: FileTypeInfo,
    context: Context,
    showSnackbar: (String) -> Unit,
    applyNodeContentUri: (Intent, NodeContentUri, String, Boolean) -> Unit,
) {
    if (isOpenWith.not() && fileTypeInfo is ZipFileTypeInfo) {
        openZipFile(
            megaNavigator = megaNavigator,
            context = context,
            localFile = file,
            fileNode = typedFileNode,
            showSnackbar = showSnackbar,
        )
    } else {
        handleOtherFiles(
            context = context,
            localFile = file,
            mimeType = fileTypeInfo.mimeType,
            showSnackbar = showSnackbar,
            applyNodeContentUri = applyNodeContentUri
        )
    }
}

private fun openZipFile(
    megaNavigator: MegaNavigator,
    context: Context,
    localFile: File,
    fileNode: TypedFileNode?,
    showSnackbar: (String) -> Unit,
) {
    Timber.d("The file is zip, open in-app.")
    megaNavigator.openZipBrowserActivity(
        context = context,
        zipFilePath = localFile.absolutePath,
        nodeHandle = fileNode?.id?.longValue,
    ) {
        showSnackbar(context.getString(sharedR.string.message_zip_format_error))
    }
}

private fun handleOtherFiles(
    context: Context,
    localFile: File,
    mimeType: String,
    showSnackbar: (String) -> Unit,
    applyNodeContentUri: (Intent, NodeContentUri, String, Boolean) -> Unit,
) {
    Intent(Intent.ACTION_VIEW).apply {
        applyNodeContentUri(
            this,
            NodeContentUri.LocalContentUri(localFile),
            mimeType,
            false
        )
        runCatching {
            context.startActivity(this)
        }.onFailure { error ->
            Timber.e(error)
            showSnackbar(context.getString(sharedR.string.intent_not_available))
        }
    }
}