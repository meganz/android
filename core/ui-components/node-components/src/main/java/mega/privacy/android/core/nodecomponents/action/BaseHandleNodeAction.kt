package mega.privacy.android.core.nodecomponents.action

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.ZipFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNodeContent
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
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
 * @param onOpenPdf callback to open PDF activity
 * @param onOpenImageViewer callback to open image viewer activity
 * @param onOpenTextEditor callback to open text editor activity
 * @param onOpenMediaPlayer callback to open media player activity
 * @param onOpenZipBrowser callback to open zip browser activity
 * @param coroutineScope [CoroutineScope]
 * @param onDownloadEvent callback for download event
 */
@Composable
fun BaseHandleNodeAction(
    typedFileNode: TypedFileNode,
    showSnackbar: (String) -> Unit,
    onActionHandled: () -> Unit,
    onOpenPdf: (NodeContentUri) -> Unit,
    onOpenImageViewer: () -> Unit,
    onOpenTextEditor: (TextEditorMode) -> Unit,
    onOpenMediaPlayer: suspend (NodeContentUri) -> Unit,
    onOpenZipBrowser: (String, Long?, () -> Unit) -> Unit,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    onDownloadEvent: (TransferTriggerEvent) -> Unit = {},
) {
    val nodeActionsViewModel: NodeActionHandlerViewModel = hiltViewModel()
    val context = LocalContext.current

    if (!typedFileNode.isNodeKeyDecrypted) {
        showSnackbar(context.getString(sharedR.string.preview_not_available_undecrypted_files))
        onActionHandled()
        return
    }

    LaunchedEffect(key1 = typedFileNode) {
        runCatching {
            nodeActionsViewModel.handleFileNodeClicked(typedFileNode)
        }.onSuccess { content ->
            when (content) {
                is FileNodeContent.Pdf -> onOpenPdf(content.uri)
                is FileNodeContent.ImageForNode -> onOpenImageViewer()
                is FileNodeContent.TextContent -> onOpenTextEditor(TextEditorMode.View)
                is FileNodeContent.AudioOrVideo -> {
                    openVideoOrAudioFile(
                        onOpenMediaPlayer = onOpenMediaPlayer,
                        context = context,
                        content = content.uri,
                        fileNode = typedFileNode,
                        showSnackbar = showSnackbar,
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
                            onOpenZipBrowser = onOpenZipBrowser,
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
    onOpenMediaPlayer: suspend (NodeContentUri) -> Unit,
    context: Context,
    fileNode: TypedFileNode,
    content: NodeContentUri,
    showSnackbar: (String) -> Unit,
    coroutineScope: CoroutineScope,
) {
    coroutineScope.launch {
        runCatching {
            onOpenMediaPlayer(content)
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