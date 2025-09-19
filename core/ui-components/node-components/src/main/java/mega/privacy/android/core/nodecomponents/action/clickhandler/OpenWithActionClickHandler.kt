package mega.privacy.android.core.nodecomponents.action.clickhandler

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.android.core.ui.model.SnackbarAttributes
import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.action.MultipleNodesActionProvider
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionProvider
import mega.privacy.android.core.nodecomponents.menu.menuaction.OpenWithMenuAction
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.file.GetFileUriUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import mega.privacy.android.domain.usecase.node.GetNodePreviewFileUseCase
import mega.privacy.android.domain.usecase.streaming.GetStreamingUriStringForNode
import mega.privacy.android.shared.resources.R as sharedResR
import timber.log.Timber
import java.io.File
import javax.inject.Inject

class OpenWithActionClickHandler @Inject constructor(
    private val getFileUriUseCase: GetFileUriUseCase,
    private val getNodePreviewFileUseCase: GetNodePreviewFileUseCase,
    private val httpServerStartUseCase: MegaApiHttpServerStartUseCase,
    private val httpServerIsRunningUseCase: MegaApiHttpServerIsRunningUseCase,
    private val getStreamingUriStringForNode: GetStreamingUriStringForNode,
) : SingleNodeAction, MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is OpenWithMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        provider.coroutineScope.launch {
            withContext(NonCancellable) {
                if (node is TypedFileNode) {
                    val file = getLocalFile(node)
                    if (node.type is AudioFileTypeInfo || node.type is VideoFileTypeInfo) {
                        openAudioOrVideoFiles(
                            context = provider.context,
                            snackbarHandler = { attr ->
                                attr.message?.let {
                                    provider.postMessage(it)
                                }
                            },
                            localFile = file,
                            node = node,
                            parentCoroutineScope = provider.coroutineScope,
                            openWithAction = {
                                provider.viewModel.downloadNodeForPreview(true)
                            }
                        )
                    } else {
                        file?.let {
                            openNotStreamableFiles(
                                context = provider.context,
                                snackbarHandler = { attr ->
                                    attr.message?.let {
                                        provider.postMessage(it)
                                    }
                                },
                                localFile = it,
                                fileTypeInfo = node.type,
                                parentCoroutineScope = provider.coroutineScope
                            )
                        } ?: run {
                            provider.viewModel.downloadNodeForPreview(true)
                        }
                    }
                } else {
                    Timber.e("Cannot do the operation open with: Node is not a FileNode")
                }
            }
        }
    }

    private suspend fun openAudioOrVideoFiles(
        context: Context,
        localFile: File?,
        node: TypedFileNode,
        parentCoroutineScope: CoroutineScope,
        snackbarHandler: (SnackbarAttributes) -> Unit,
        openWithAction: () -> Unit,
    ) {
        val fileUri = getAudioOrVideoFileUri(context, localFile, node)
        Intent(Intent.ACTION_VIEW).apply {
            if (fileUri != null) {
                setDataAndType(Uri.parse(fileUri), node.type.mimeType)
            } else {
                snackbarHandler(
                    SnackbarAttributes(
                        message = context.getString(
                            sharedResR.string.error_open_file_with
                        )
                    )
                )
            }
            if (resolveActivity(context.packageManager) != null) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                parentCoroutineScope.ensureActive()
                context.startActivity(this)
            } else if (localFile == null) {
                parentCoroutineScope.ensureActive()
                openWithAction()
            } else {
                snackbarHandler(
                    SnackbarAttributes(
                        message = context.getString(
                            sharedResR.string.intent_not_available_file
                        )
                    )
                )
            }
        }
    }

    private suspend fun openNotStreamableFiles(
        context: Context,
        localFile: File?,
        fileTypeInfo: FileTypeInfo,
        parentCoroutineScope: CoroutineScope,
        snackbarHandler: (SnackbarAttributes) -> Unit,
    ) {
        val localFileUri = getLocalFileUri(localFile, context)
        Intent(Intent.ACTION_VIEW).apply {
            localFileUri?.let {
                setDataAndType(Uri.parse(it), fileTypeInfo.mimeType)
                if (resolveActivity(context.packageManager) == null) {
                    action = Intent.ACTION_SEND
                }
                if (resolveActivity(context.packageManager) != null) {
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    parentCoroutineScope.ensureActive()
                    context.startActivity(this@apply)
                } else {
                    snackbarHandler(
                        SnackbarAttributes(
                            message = context.getString(sharedResR.string.intent_not_available)
                        )
                    )
                }
            } ?: run {
                snackbarHandler(
                    SnackbarAttributes(
                        message = context.getString(sharedResR.string.general_text_error)
                    )
                )
            }
        }
    }

    private suspend fun getAudioOrVideoFileUri(
        context: Context,
        localFile: File?,
        node: TypedFileNode,
    ): String? = localFile?.let {
        getLocalFileUri(it, context)
    } ?: run {
        if (httpServerRunning() == 0) {
            startHttpServer()
        }
        getStreamingUri(node)
    }

    private suspend fun getLocalFileUri(file: File?, context: Context) = runCatching {
        val fileProviderAuthority = context.packageName + ".providers.fileprovider"
        file?.let { getFileUriUseCase(it, fileProviderAuthority) }
    }.onFailure { Timber.e("Error getting local file uri: ${it.message}") }.getOrNull()

    private suspend fun getLocalFile(node: TypedFileNode): File? = runCatching {
        getNodePreviewFileUseCase(node)
    }.onFailure { Timber.e("Error getting local file path: ${it.message}") }.getOrNull()

    private suspend fun getStreamingUri(node: TypedFileNode) = runCatching {
        getStreamingUriStringForNode(node)
    }.onFailure { Timber.e("Error getting streaming uri: ${it.message}") }.getOrNull()

    private suspend fun startHttpServer() = runCatching {
        httpServerStartUseCase()
    }.onFailure { Timber.e("Error starting http server: ${it.message}") }.getOrNull()

    private suspend fun httpServerRunning() = runCatching {
        httpServerIsRunningUseCase()
    }.onFailure { Timber.e("Error checking if http server is running: ${it.message}") }
        .getOrDefault(0)

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        provider.viewModel.downloadNodeForPreview(true)
    }
}
