package mega.privacy.android.app.presentation.offline.action

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.EntryPointAccessors
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.MimeTypeList.Companion.typeForName
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewActivity
import mega.privacy.android.app.presentation.imagepreview.fetcher.OfflineImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewMenuSource
import mega.privacy.android.app.presentation.offline.action.model.OfflineNodeActionUiEntity
import mega.privacy.android.app.presentation.pdfviewer.PdfViewerActivity
import mega.privacy.android.app.textEditor.TextEditorActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.MegaNodeUtil.MegaNavigatorEntryPoint
import mega.privacy.android.domain.entity.SortOrder
import timber.log.Timber
import java.io.File
import java.util.UUID

/**
 * Composable function to handle offline node share action clicks
 */
@Composable
fun HandleOfflineNodeActions(
    viewModel: OfflineNodeActionsViewModel,
    snackBarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope,
    sortOrder: SortOrder = SortOrder.ORDER_NONE,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    EventEffect(
        event = uiState.shareFilesEvent,
        onConsumed = viewModel::onShareFilesEventConsumed
    ) {
        startShareFilesIntent(context = context, files = it)
    }

    EventEffect(
        event = uiState.sharesNodeLinksEvent,
        onConsumed = viewModel::onShareNodeLinksEventConsumed
    ) {
        startShareLinksIntent(context = context, title = it.first, links = it.second)
    }

    EventEffect(
        event = uiState.openFileEvent,
        onConsumed = viewModel::onOpenFileEventConsumed
    ) {
        coroutineScope.launch {
            openFile(
                context = context,
                content = it,
                applyNodeContentUri = viewModel::applyNodeContentUri,
                snackBarHostState = snackBarHostState,
                coroutineScope = coroutineScope,
                sortOrder = sortOrder
            )
        }
    }
}

private fun startShareFilesIntent(context: Context, files: List<File>) {
    var intentType: String? = null
    for (file in files) {
        val type = typeForName(file.getName()).type
        if (intentType == null) {
            intentType = type
        } else if (!TextUtils.equals(intentType, type)) {
            intentType = "*"
            break
        }
    }
    val uris = ArrayList<Uri>()
    for (file in files) {
        uris.add(FileUtil.getUriForFile(context, file))
    }
    val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE)
    shareIntent.setType("$intentType/*")
    shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
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
        type = Constants.TYPE_TEXT_PLAIN
    }
    context.startActivity(
        Intent.createChooser(
            shareIntent,
            context.getString(R.string.context_share)
        )
    )
}

/**
 * Typealias for applyNodeContentUri function
 */
private typealias ApplyNodeContentUri = (intent: Intent, localFile: File, mimeType: String, isSupported: Boolean) -> Unit

/**
 * Opens the file
 */
private suspend fun openFile(
    context: Context,
    content: OfflineNodeActionUiEntity,
    applyNodeContentUri: ApplyNodeContentUri,
    snackBarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope,
    sortOrder: SortOrder,
) {
    when (content) {
        is OfflineNodeActionUiEntity.Image -> openImageViewerActivity(
            context = context,
            content = content,
            snackBarHostState = snackBarHostState
        )

        is OfflineNodeActionUiEntity.AudioOrVideo -> openVideoOrAudioFile(
            context = context,
            content = content,
            snackBarHostState = snackBarHostState,
            sortOrder = sortOrder,
            coroutineScope = coroutineScope
        )

        is OfflineNodeActionUiEntity.Pdf -> openPdfActivity(
            context = context,
            content = content,
            applyNodeContentUri = applyNodeContentUri,
        )

        is OfflineNodeActionUiEntity.Text -> openTextEditorActivity(
            context = context,
            content = content,
            snackBarHostState = snackBarHostState
        )

        is OfflineNodeActionUiEntity.Uri -> openUrlFile(
            context = context,
            content = content,
            snackBarHostState = snackBarHostState
        )

        is OfflineNodeActionUiEntity.Zip -> {
            openZipFile(
                context = context,
                content = content,
                snackBarHostState = snackBarHostState,
                coroutineScope = coroutineScope
            )
        }

        is OfflineNodeActionUiEntity.Other -> {
            handleOtherFiles(
                context = context,
                content = content,
                snackBarHostState = snackBarHostState,
                applyNodeContentUri = applyNodeContentUri
            )
        }
    }
}

private suspend fun openZipFile(
    context: Context,
    content: OfflineNodeActionUiEntity.Zip,
    snackBarHostState: SnackbarHostState?,
    coroutineScope: CoroutineScope,
) {
    EntryPointAccessors.fromApplication(
        context,
        MegaNavigatorEntryPoint::class.java
    ).megaNavigator().openZipBrowserActivity(
        context = context,
        zipFilePath = content.file.absolutePath,
        nodeHandle = content.nodeId.longValue,
    ) {
        coroutineScope.launch {
            snackBarHostState?.showSnackbar(context.getString(R.string.message_zip_format_error))
        }
    }
}

private suspend fun openImageViewerActivity(
    context: Context,
    content: OfflineNodeActionUiEntity.Image,
    snackBarHostState: SnackbarHostState?,
) {
    val intent = ImagePreviewActivity.createIntent(
        context = context,
        imageSource = ImagePreviewFetcherSource.OFFLINE,
        menuOptionsSource = ImagePreviewMenuSource.OFFLINE,
        anchorImageNodeId = content.nodeId,
        params = mapOf(OfflineImageNodeFetcher.PATH to content.path),
    )
    safeLaunchActivity(
        context = context,
        intent = intent,
        snackBarHostState = snackBarHostState
    )
}

private suspend fun openTextEditorActivity(
    context: Context,
    content: OfflineNodeActionUiEntity.Text,
    snackBarHostState: SnackbarHostState?,
) {
    val intent = Intent(context, TextEditorActivity::class.java).apply {
        putExtra(Constants.INTENT_EXTRA_KEY_FILE_NAME, content.file.name)
        putExtra(Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE, Constants.OFFLINE_ADAPTER)
        putExtra(Constants.INTENT_EXTRA_KEY_PATH, content.file.absolutePath)
    }
    safeLaunchActivity(
        context = context,
        intent = intent,
        snackBarHostState = snackBarHostState
    )
}

private suspend fun handleOtherFiles(
    context: Context,
    content: OfflineNodeActionUiEntity.Other,
    snackBarHostState: SnackbarHostState?,
    applyNodeContentUri: ApplyNodeContentUri,
) {
    Intent(Intent.ACTION_VIEW).apply {
        applyNodeContentUri(this, content.file, content.mimeType, false)
        runCatching {
            context.startActivity(this)
        }.onFailure { error ->
            Timber.e(error)
            if (resolveActivity(context.packageManager) == null) {
                action = Intent.ACTION_SEND
            }
            safeLaunchActivity(
                context = context,
                intent = this,
                snackBarHostState = snackBarHostState
            )
        }
    }
}

private suspend fun openUrlFile(
    context: Context,
    content: OfflineNodeActionUiEntity.Uri,
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

private fun openPdfActivity(
    context: Context,
    content: OfflineNodeActionUiEntity.Pdf,
    applyNodeContentUri: ApplyNodeContentUri,
) {
    val pdfIntent = Intent(context, PdfViewerActivity::class.java)
    pdfIntent.apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        putExtra(Constants.INTENT_EXTRA_KEY_INSIDE, true)
        putExtra(Constants.INTENT_EXTRA_KEY_HANDLE, content.nodeId.longValue)
        putExtra(Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE, Constants.OFFLINE_ADAPTER)
        pdfIntent.putExtra(Constants.INTENT_EXTRA_KEY_PATH, content.file.absolutePath)
        putExtra(Constants.INTENT_EXTRA_KEY_APP, true)
    }
    applyNodeContentUri(pdfIntent, content.file, content.mimeType, true)
    context.startActivity(pdfIntent)
}

private suspend fun openVideoOrAudioFile(
    context: Context,
    content: OfflineNodeActionUiEntity.AudioOrVideo,
    snackBarHostState: SnackbarHostState?,
    sortOrder: SortOrder,
    coroutineScope: CoroutineScope,
) {
    EntryPointAccessors.fromApplication(context, MegaNavigatorEntryPoint::class.java)
        .megaNavigator().openMediaPlayerActivityByLocalFile(
            context = context,
            localFile = content.file,
            fileTypeInfo = content.fileTypeInfo,
            viewType = Constants.OFFLINE_ADAPTER,
            handle = content.nodeId.longValue,
            parentId = content.parentId.toLong(),
            sortOrder = sortOrder,
        ) {
            coroutineScope.launch {
                snackBarHostState?.showSnackbar(message = context.getString(R.string.intent_not_available))
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
        Timber.e(it)
        snackBarHostState?.showSnackbar(message = context.getString(R.string.intent_not_available))
    }
}