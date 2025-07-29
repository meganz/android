package mega.privacy.android.app.presentation.node.action

import android.content.Context
import android.content.Intent
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.snackbar.SnackbarHostStateWrapper
import mega.privacy.android.app.presentation.snackbar.showAutoDurationSnackbar
import mega.privacy.android.app.textEditor.TextEditorViewModel
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.ZipFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNodeContent
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.navigation.megaNavigator
import timber.log.Timber
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
    HandleNodeAction(
        typedFileNode = typedFileNode,
        showSnackbar = { message ->
            coroutineScope.launch {
                snackbarHostStateWrapper.showAutoDurationSnackbar(message)
            }
        },
        onActionHandled = onActionHandled,
        coroutineScope = coroutineScope,
        nodeSourceType = nodeSourceType,
        onDownloadEvent = onDownloadEvent,
        sortOrder = sortOrder
    )
}

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
fun HandleNodeAction(
    typedFileNode: TypedFileNode,
    showSnackbar: (String) -> Unit,
    onActionHandled: () -> Unit,
    coroutineScope: CoroutineScope,
    nodeSourceType: Int? = null,
    onDownloadEvent: (TransferTriggerEvent) -> Unit = {},
    sortOrder: SortOrder = SortOrder.ORDER_NONE,
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
                    currentFileNode = typedFileNode,
                    mode = TextEditorViewModel.VIEW_MODE,
                    nodeSourceType = nodeSourceType,
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
                            nodeActionsViewModel = nodeActionsViewModel,
                            showSnackbar = showSnackbar,
                            coroutineScope = coroutineScope,
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


@Composable
fun HandleFileAction(
    file: File,
    isOpenWith: Boolean,
    snackBarHostState: SnackbarHostStateWrapper?,
    onActionHandled: () -> Unit,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
) {
    val nodeActionsViewModel: NodeActionHandlerViewModel = hiltViewModel()
    val context = LocalContext.current
    val megaNavigator = remember { context.megaNavigator }

    LaunchedEffect(file) {
        openOtherFile(
            megaNavigator = megaNavigator,
            file = file,
            typedFileNode = null,
            isOpenWith = isOpenWith,
            fileTypeInfo = nodeActionsViewModel.getTypeInfo(file),
            nodeActionsViewModel = nodeActionsViewModel,
            showSnackbar = { message ->
                coroutineScope.launch {
                    snackBarHostState?.showAutoDurationSnackbar(message)
                }
            },
            coroutineScope = coroutineScope,
            context = context,
        )

        onActionHandled()
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
            showSnackbar(context.getString(R.string.intent_not_available))
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
        showSnackbar(context.getString(R.string.general_text_error))
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
        showSnackbar(context.getString(R.string.intent_not_available))
    }
}

private fun Intent.openShareIntent(
    context: Context,
    showSnackbar: (String) -> Unit,
) {
    if (resolveActivity(context.packageManager) == null) {
        action = Intent.ACTION_SEND
    }
    safeLaunchActivity(
        context = context,
        intent = this,
        showSnackbar = showSnackbar
    )
}

private fun openOtherFile(
    megaNavigator: MegaNavigator,
    file: File,
    typedFileNode: TypedFileNode?,
    isOpenWith: Boolean,
    fileTypeInfo: FileTypeInfo,
    nodeActionsViewModel: NodeActionHandlerViewModel,
    showSnackbar: (String) -> Unit,
    coroutineScope: CoroutineScope,
    context: Context,
) {
    if (isOpenWith.not() && fileTypeInfo is ZipFileTypeInfo) {
        openZipFile(
            megaNavigator = megaNavigator,
            context = context,
            localFile = file,
            fileNode = typedFileNode,
            showSnackbar = showSnackbar,
            coroutineScope = coroutineScope
        )
    } else {
        handleOtherFiles(
            context = context,
            localFile = file,
            mimeType = fileTypeInfo.mimeType,
            showSnackbar = showSnackbar,
            nodeActionsViewModel = nodeActionsViewModel
        )
    }
}

private fun openZipFile(
    megaNavigator: MegaNavigator,
    context: Context,
    localFile: File,
    fileNode: TypedFileNode?,
    showSnackbar: (String) -> Unit,
    coroutineScope: CoroutineScope,
) {
    Timber.d("The file is zip, open in-app.")
    megaNavigator.openZipBrowserActivity(
        context = context,
        zipFilePath = localFile.absolutePath,
        nodeHandle = fileNode?.id?.longValue,
    ) {
        coroutineScope.launch {
            showSnackbar(context.getString(R.string.message_zip_format_error))
        }
    }
}

private fun handleOtherFiles(
    context: Context,
    localFile: File,
    mimeType: String,
    showSnackbar: (String) -> Unit,
    nodeActionsViewModel: NodeActionHandlerViewModel,
) {
    Intent(Intent.ACTION_VIEW).apply {
        nodeActionsViewModel.applyNodeContentUri(
            intent = this,
            content = NodeContentUri.LocalContentUri(localFile),
            mimeType = mimeType,
            isSupported = false
        )
        runCatching {
            context.startActivity(this)
        }.onFailure { error ->
            Timber.e(error)
            openShareIntent(context = context, showSnackbar = showSnackbar)
        }
    }
}
