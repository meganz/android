package mega.privacy.android.app.presentation.node.action

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewActivity
import mega.privacy.android.app.presentation.imagepreview.fetcher.CloudDriveImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.fetcher.RubbishBinImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.fetcher.SharedItemsImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewMenuSource
import mega.privacy.android.app.presentation.node.FileNodeContent
import mega.privacy.android.app.presentation.node.NodeActionsViewModel
import mega.privacy.android.app.presentation.pdfviewer.PdfViewerActivity
import mega.privacy.android.app.textEditor.TextEditorActivity
import mega.privacy.android.app.textEditor.TextEditorViewModel
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.MegaNodeUtil.MegaNavigatorEntryPoint
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.ZipFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.TypedFileNode
import timber.log.Timber
import java.io.File

/**
 * Handle node action click
 *
 * @param typedFileNode [TypedFileNode]
 * @param nodeSourceType from where item click is performed
 * @param nodeActionsViewModel [NodeActionsViewModel]
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
    nodeActionsViewModel: NodeActionsViewModel = hiltViewModel(),
    sortOrder: SortOrder = SortOrder.ORDER_NONE,
) {
    val context = LocalContext.current

    LaunchedEffect(key1 = typedFileNode) {
        runCatching {
            nodeActionsViewModel.handleFileNodeClicked(typedFileNode)
        }.onSuccess { content ->
            when (content) {
                is FileNodeContent.Pdf -> openPdfActivity(
                    context = context,
                    type = nodeSourceType,
                    content = content.uri,
                    currentFileNode = typedFileNode,
                    nodeActionsViewModel = nodeActionsViewModel
                )

                is FileNodeContent.ImageForNode -> {
                    openImageViewerActivity(
                        context = context,
                        currentFileNode = typedFileNode,
                        nodeSourceType = nodeSourceType,
                    )
                }

                is FileNodeContent.TextContent -> openTextEditorActivity(
                    context = context,
                    currentFileNode = typedFileNode,
                    nodeSourceType = nodeSourceType ?: Constants.FILE_BROWSER_ADAPTER
                )

                is FileNodeContent.AudioOrVideo -> {
                    openVideoOrAudioFile(
                        context = context,
                        content = content.uri,
                        fileNode = typedFileNode,
                        snackBarHostState = snackBarHostState,
                        sortOrder = sortOrder,
                        viewType = nodeSourceType ?: Constants.FILE_BROWSER_ADAPTER,
                        coroutineScope = coroutineScope
                    )
                }

                is FileNodeContent.UrlContent -> {
                    openUrlFile(
                        context = context, content = content, snackBarHostState = snackBarHostState
                    )
                }

                is FileNodeContent.Other -> {
                    content.localFile?.let {
                        if (typedFileNode.type is ZipFileTypeInfo) {
                            openZipFile(
                                context = context,
                                localFile = it,
                                fileNode = typedFileNode,
                                snackBarHostState = snackBarHostState,
                                coroutineScope = coroutineScope
                            )
                        } else {
                            handleOtherFiles(
                                context = context,
                                localFile = it,
                                currentFileNode = typedFileNode,
                                snackBarHostState = snackBarHostState,
                                nodeActionsViewModel = nodeActionsViewModel
                            )
                        }
                    } ?: run {
                        nodeActionsViewModel.downloadNodeForPreview(typedFileNode)
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

private fun openTextEditorActivity(
    context: Context,
    currentFileNode: TypedFileNode,
    nodeSourceType: Int,
) {
    val textFileIntent = Intent(context, TextEditorActivity::class.java)
    textFileIntent.putExtra(Constants.INTENT_EXTRA_KEY_HANDLE, currentFileNode.id.longValue)
        .putExtra(TextEditorViewModel.MODE, TextEditorViewModel.VIEW_MODE)
        .putExtra(Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE, nodeSourceType)
    context.startActivity(textFileIntent)
}

private fun openPdfActivity(
    context: Context,
    content: NodeContentUri,
    type: Int?,
    currentFileNode: TypedFileNode,
    nodeActionsViewModel: NodeActionsViewModel,
) {
    val pdfIntent = Intent(context, PdfViewerActivity::class.java)
    val mimeType = currentFileNode.type.mimeType
    pdfIntent.apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        putExtra(Constants.INTENT_EXTRA_KEY_HANDLE, currentFileNode.id.longValue)
        putExtra(Constants.INTENT_EXTRA_KEY_INSIDE, true)
        putExtra(Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE, type)
        putExtra(Constants.INTENT_EXTRA_KEY_APP, true)
    }
    nodeActionsViewModel.applyNodeContentUri(
        intent = pdfIntent,
        content = content,
        mimeType = mimeType,
    )
    context.startActivity(pdfIntent)
}

private fun openImageViewerActivity(
    context: Context,
    currentFileNode: TypedFileNode,
    nodeSourceType: Int?,
) {
    val currentFileNodeParentId = currentFileNode.parentId.longValue

    val (imageSource, menuOptionsSource, paramKey) = when (nodeSourceType) {
        Constants.FILE_BROWSER_ADAPTER -> Triple(
            ImagePreviewFetcherSource.CLOUD_DRIVE,
            ImagePreviewMenuSource.CLOUD_DRIVE,
            CloudDriveImageNodeFetcher.PARENT_ID
        )

        Constants.RUBBISH_BIN_ADAPTER -> Triple(
            ImagePreviewFetcherSource.RUBBISH_BIN,
            ImagePreviewMenuSource.RUBBISH_BIN,
            RubbishBinImageNodeFetcher.PARENT_ID
        )

        Constants.INCOMING_SHARES_ADAPTER,
        Constants.OUTGOING_SHARES_ADAPTER,
        -> Triple(
            ImagePreviewFetcherSource.SHARED_ITEMS,
            ImagePreviewMenuSource.SHARED_ITEMS,
            SharedItemsImageNodeFetcher.PARENT_ID
        )

        Constants.LINKS_ADAPTER -> Triple(
            ImagePreviewFetcherSource.SHARED_ITEMS,
            ImagePreviewMenuSource.LINKS,
            SharedItemsImageNodeFetcher.PARENT_ID
        )

        Constants.BACKUPS_ADAPTER -> Triple(
            ImagePreviewFetcherSource.CLOUD_DRIVE,
            ImagePreviewMenuSource.CLOUD_DRIVE,
            CloudDriveImageNodeFetcher.PARENT_ID
        )

        else -> {
            Timber.e("Unknown node source type: $nodeSourceType")
            return
        }
    }

    val intent = ImagePreviewActivity.createIntent(
        context = context,
        imageSource = imageSource,
        menuOptionsSource = menuOptionsSource,
        anchorImageNodeId = currentFileNode.id,
        params = mapOf(paramKey to currentFileNodeParentId),
    )

    context.startActivity(intent)
}

private suspend fun openVideoOrAudioFile(
    context: Context,
    fileNode: TypedFileNode,
    content: NodeContentUri,
    snackBarHostState: SnackbarHostState?,
    sortOrder: SortOrder,
    viewType: Int,
    coroutineScope: CoroutineScope,
) {
    EntryPointAccessors.fromApplication(context, MegaNavigatorEntryPoint::class.java)
        .megaNavigator().openMediaPlayerActivityByFileNode(
            context = context,
            contentUri = content,
            fileNode = fileNode,
            sortOrder = sortOrder,
            viewType = viewType,
            isFolderLink = false,
            onError = {
                coroutineScope.launch {
                    snackBarHostState?.showSnackbar(context.getString(R.string.intent_not_available))
                }
            }
        )
}

private suspend fun openUrlFile(
    context: Context,
    content: FileNodeContent.UrlContent,
    snackBarHostState: SnackbarHostState?,
) {
    content.path?.let {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(it)
        }
        safeLaunchActivity(
            context = context,
            intent = intent,
            snackBarHostState = snackBarHostState
        )
    } ?: run {
        snackBarHostState?.showSnackbar(message = context.getString(R.string.general_text_error))
    }
}

private suspend fun safeLaunchActivity(
    context: Context,
    intent: Intent,
    snackBarHostState: SnackbarHostState?,
) {
    runCatching {
        context.startActivity(intent)
    }.onFailure {
        Timber.e(it)
        snackBarHostState?.showSnackbar(message = context.getString(R.string.intent_not_available))
    }
}

private suspend fun Intent.openShareIntent(
    context: Context,
    snackBarHostState: SnackbarHostState?,
) {
    if (resolveActivity(context.packageManager) == null) {
        action = Intent.ACTION_SEND
    }
    safeLaunchActivity(
        context = context,
        intent = this,
        snackBarHostState = snackBarHostState
    )
}

private fun openZipFile(
    context: Context,
    localFile: File,
    fileNode: TypedFileNode,
    snackBarHostState: SnackbarHostState?,
    coroutineScope: CoroutineScope,
) {
    Timber.d("The file is zip, open in-app.")
    EntryPointAccessors.fromApplication(
        context,
        MegaNavigatorEntryPoint::class.java
    ).megaNavigator().openZipBrowserActivity(
        context = context,
        zipFilePath = localFile.absolutePath,
        nodeHandle = fileNode.id.longValue,
    ) {
        coroutineScope.launch {
            snackBarHostState?.showSnackbar(context.getString(R.string.message_zip_format_error))
        }
    }
}

private suspend fun handleOtherFiles(
    context: Context,
    localFile: File,
    currentFileNode: TypedFileNode,
    snackBarHostState: SnackbarHostState?,
    nodeActionsViewModel: NodeActionsViewModel,
) {
    Intent(Intent.ACTION_VIEW).apply {
        nodeActionsViewModel.applyNodeContentUri(
            intent = this,
            content = NodeContentUri.LocalContentUri(localFile),
            mimeType = currentFileNode.type.mimeType,
            isSupported = false
        )
        runCatching {
            context.startActivity(this)
        }.onFailure { error ->
            Timber.e(error)
            openShareIntent(context = context, snackBarHostState = snackBarHostState)
        }
    }
}
