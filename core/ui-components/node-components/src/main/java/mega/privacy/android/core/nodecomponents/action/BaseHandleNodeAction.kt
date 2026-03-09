package mega.privacy.android.core.nodecomponents.action

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.ZipFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNodeContent
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFile
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.shared.resources.R as sharedR
import timber.log.Timber
import java.io.File

/**
 * Handle node action click with navigation lambdas
 *
 * @param typedFileNode [TypedFileNode]
 * @param showSnackbar callback to show snackbar messages
 * @param onActionHandled callback after file clicked
 * @param onOpenFileContent
 * @param onDownloadEvent callback for download event
 */
@Composable
fun BaseHandleNodeAction(
    typedFileNode: TypedFileNode,
    showSnackbar: (String) -> Unit,
    onActionHandled: () -> Unit,
    onOpenFileContent: (FileNodeContent, Boolean, Boolean) -> Unit,
    onDownloadEvent: (TransferTriggerEvent) -> Unit = {},
    isLinkNode: Boolean = false,
) {
    val nodeActionsViewModel: NodeActionHandlerViewModel = hiltViewModel()
    val resources = LocalResources.current
    val context = LocalContext.current

    if (!typedFileNode.isNodeKeyDecrypted) {
        showSnackbar(resources.getString(sharedR.string.preview_not_available_undecrypted_files))
        onActionHandled()
        return
    }

    LaunchedEffect(key1 = typedFileNode) {
        runCatching {
            nodeActionsViewModel.handleFileNodeClicked(typedFileNode, isLinkNode)
        }.onSuccess { content ->
            runCatching {
                when (content) {
                    is FileNodeContent.TextContent -> {
                        val isTextEditorComposeEnabled = runCatching {
                            nodeActionsViewModel.state
                                .map { it.isTextEditorComposeEnabled }
                                .first { it != null }
                        }.getOrDefault(false)

                        onOpenFileContent(
                            content,
                            isTextEditorComposeEnabled ?: false,
                            false
                        )
                    }

                    is FileNodeContent.Pdf -> {
                        val isPDFViewerEnabled = runCatching {
                            nodeActionsViewModel.state
                                .map { it.isPDFViewerEnabled }
                                .first { it != null }
                        }.getOrDefault(false)

                        onOpenFileContent(
                            content,
                            false,
                            isPDFViewerEnabled ?: false
                        )
                    }

                    is FileNodeContent.Other -> {
                        content.localFile.let { localFile ->
                            if (localFile == null) {
                                onDownloadEvent(
                                    TransferTriggerEvent.StartDownloadForPreview(
                                        node = if (isLinkNode) {
                                            PublicLinkFile(typedFileNode, null)
                                        } else {
                                            typedFileNode
                                        },
                                        isOpenWith = false
                                    )
                                )
                            } else if (typedFileNode.type !is ZipFileTypeInfo) {
                                handleOtherFiles(
                                    context = context,
                                    localFile = localFile,
                                    mimeType = typedFileNode.type.mimeType,
                                    showSnackbar = showSnackbar,
                                    applyNodeContentUri = nodeActionsViewModel::applyNodeContentUri
                                )
                            } else {
                                onOpenFileContent(
                                    FileNodeContent.LocalZipFile(localFile),
                                    false,
                                    false
                                )
                            }
                        }
                    }

                    is FileNodeContent.UrlContent -> {
                        openUrlFile(context, content, showSnackbar)
                    }

                    else -> {
                        onOpenFileContent(content, false, false)
                    }
                }
            }.onFailure {
                showSnackbar(resources.getString(sharedR.string.intent_not_available))
            }
            onActionHandled()
        }.onFailure {
            Timber.e(it)
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

/**
 * Open other file type
 */
fun openOtherFile(
    onOpenZipBrowser: (String, Long?, () -> Unit) -> Unit,
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
            onOpenZipBrowser = onOpenZipBrowser,
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
    onOpenZipBrowser: (String, Long?, () -> Unit) -> Unit,
    context: Context,
    localFile: File,
    fileNode: TypedFileNode?,
    showSnackbar: (String) -> Unit,
) {
    Timber.d("The file is zip, open in-app.")
    onOpenZipBrowser(
        localFile.absolutePath,
        fileNode?.id?.longValue,
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
