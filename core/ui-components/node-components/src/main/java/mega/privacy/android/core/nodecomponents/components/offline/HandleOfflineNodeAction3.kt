package mega.privacy.android.core.nodecomponents.components.offline

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.privacy.android.core.nodecomponents.R
import mega.privacy.android.core.nodecomponents.model.NodeSourceTypeInt
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.navigation.extensions.rememberMegaNavigator
import mega.privacy.android.shared.resources.R as sharedR
import java.io.File
import java.util.UUID

/**
 * Composable function to handle offline node action clicks
 */
@Composable
fun HandleOfflineNodeAction3(
    uiState: OfflineNodeActionsUiState,
    sortOrder: SortOrder = SortOrder.ORDER_NONE,
    consumeShareFilesEvent: () -> Unit = {},
    consumeShareNodeLinksEvent: () -> Unit = {},
    consumeOpenFileEvent: () -> Unit = {},
) {
    val context = LocalContext.current
    val megaNavigator = rememberMegaNavigator()
    val snackBarHostState = LocalSnackBarHostState.current
    val coroutineScope = rememberCoroutineScope()


    EventEffect(
        event = uiState.shareFilesEvent,
        onConsumed = consumeShareFilesEvent
    ) {
        startShareFilesIntent(context = context, files = it)
    }

    EventEffect(
        event = uiState.sharesNodeLinksEvent,
        onConsumed = consumeShareNodeLinksEvent
    ) {
        startShareLinksIntent(
            context = context,
            title = it.first,
            links = it.second
        )
    }

    EventEffect(
        event = uiState.openFileEvent,
        onConsumed = consumeOpenFileEvent
    ) {
        coroutineScope.launch {
            openFile(
                context = context,
                content = it,
                snackBarHostState = snackBarHostState,
                coroutineScope = coroutineScope,
                sortOrder = sortOrder,
                megaNavigator = megaNavigator,
            )
        }
    }
}

private fun startShareFilesIntent(context: Context, files: List<File>) {
    val uris = files.map(File::toUri)
    val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = "*/*"
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(
        Intent.createChooser(shareIntent, context.getString(R.string.context_share))
    )
}

private fun startShareLinksIntent(context: Context, title: String?, links: String) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_TEXT, links)
        title?.let {
            putExtra(Intent.EXTRA_SUBJECT, it)
        } ?: run {
            val uniqueId = UUID.randomUUID()
            putExtra(Intent.EXTRA_SUBJECT, "${uniqueId}.url")
        }
        type = "text/plain"
    }
    context.startActivity(
        Intent.createChooser(
            shareIntent,
            context.getString(R.string.context_share)
        )
    )
}

/**
 * Opens the file using simplified approach that works with available dependencies
 */
private suspend fun openFile(
    context: Context,
    content: OfflineNodeActionUiEntity,
    snackBarHostState: SnackbarHostState?,
    coroutineScope: CoroutineScope,
    sortOrder: SortOrder,
    megaNavigator: MegaNavigator,
) {
    when (content) {
        is OfflineNodeActionUiEntity.Image -> {
            megaNavigator.openImageViewerForOfflineNode(
                context = context,
                node = content.nodeId,
                path = content.path,
            )
        }

        is OfflineNodeActionUiEntity.AudioOrVideo -> {
            coroutineScope.launch {
                runCatching {
                    megaNavigator.openMediaPlayerActivityByLocalFile(
                        context = context,
                        localFile = content.file,
                        fileTypeInfo = content.fileTypeInfo,
                        viewType = 2004, // Constants.OFFLINE_ADAPTER
                        handle = content.nodeId.longValue,
                        offlineParentId = content.parentId,
                        sortOrder = sortOrder,
                    )
                }.onFailure {
                    snackBarHostState?.showAutoDurationSnackbar(
                        context.getString(sharedR.string.intent_not_available)
                    )
                }
            }
        }

        is OfflineNodeActionUiEntity.Pdf -> {
            megaNavigator.openPdfActivity(
                context = context,
                content = NodeContentUri.LocalContentUri(content.file),
                type = NodeSourceTypeInt.FILE_BROWSER_ADAPTER,
                nodeId = content.nodeId
            )
        }

        is OfflineNodeActionUiEntity.Text -> {
            megaNavigator.openTextEditorActivity(
                context = context,
                currentNodeId = content.nodeId,
                nodeSourceType = NodeSourceTypeInt.FILE_BROWSER_ADAPTER,
                mode = TextEditorMode.Edit
            )
        }

        is OfflineNodeActionUiEntity.Uri -> {
            content.path?.let { path ->
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = path.toUri()
                }
                safeLaunchActivity(
                    context = context,
                    intent = intent,
                    snackBarHostState = snackBarHostState
                )
            } ?: run {
                snackBarHostState?.showAutoDurationSnackbar(
                    context.getString(sharedR.string.general_text_error)
                )
            }
        }

        is OfflineNodeActionUiEntity.Zip -> {
            megaNavigator.openZipBrowserActivity(
                context = context,
                zipFilePath = content.file.absolutePath,
                nodeHandle = content.nodeId.longValue,
            ) {
                coroutineScope.launch {
                    snackBarHostState?.showAutoDurationSnackbar(
                        context.getString(sharedR.string.message_zip_format_error)
                    )
                }
            }
        }

        is OfflineNodeActionUiEntity.Other -> {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.fromFile(content.file), content.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            safeLaunchActivity(
                context = context,
                intent = intent,
                snackBarHostState = snackBarHostState
            )
        }
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
        snackBarHostState?.showAutoDurationSnackbar(
            context.getString(sharedR.string.intent_not_available)
        )
    }
}
