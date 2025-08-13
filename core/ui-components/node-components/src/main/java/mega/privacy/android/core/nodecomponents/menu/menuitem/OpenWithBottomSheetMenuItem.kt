package mega.privacy.android.core.nodecomponents.menu.menuitem

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.navigation.NavHostController
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.action.NodeActionHandler
import mega.privacy.android.core.nodecomponents.menu.menuaction.OpenWithMenuAction
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.file.GetFileUriUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import mega.privacy.android.domain.usecase.node.GetNodePreviewFileUseCase
import mega.privacy.android.domain.usecase.streaming.GetStreamingUriStringForNode
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Open with bottom sheet menu item
 *
 * @param menuAction [OpenWithMenuAction]
 * @param getFileUriUseCase [mega.privacy.android.domain.usecase.file.GetFileUriUseCase]
 * @param getNodePreviewFileUseCase [mega.privacy.android.domain.usecase.node.GetNodePreviewFileUseCase]
 * @param httpServerStartUseCase [mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase]
 * @param getStreamingUriStringForNode [mega.privacy.android.domain.usecase.streaming.GetStreamingUriStringForNode]
 * @param snackBarHandler [SnackBarHandler]
 * @param context [android.content.Context]
 */
class OpenWithBottomSheetMenuItem @Inject constructor(
    override val menuAction: OpenWithMenuAction,
    private val getFileUriUseCase: GetFileUriUseCase,
    private val getNodePreviewFileUseCase: GetNodePreviewFileUseCase,
    private val httpServerStartUseCase: MegaApiHttpServerStartUseCase,
    private val httpServerIsRunningUseCase: MegaApiHttpServerIsRunningUseCase,
    private val getStreamingUriStringForNode: GetStreamingUriStringForNode,
    // Todo provide snackbar
    //private val snackBarHandler: SnackBarHandler,
    @ApplicationContext private val context: Context,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ) = node is TypedFileNode
            && node.isTakenDown.not()
            && isNodeInRubbish.not()

    override fun getOnClickFunction(
        node: TypedNode,
        onDismiss: () -> Unit,
        actionHandler: NodeActionHandler,
        navController: NavHostController,
        parentCoroutineScope: CoroutineScope,
    ): () -> Unit = {
        if (node is TypedFileNode) {
            parentCoroutineScope.launch {
                withContext(NonCancellable) {
                    val file = getLocalFile(node)
                    if (node.type is AudioFileTypeInfo || node.type is VideoFileTypeInfo) {
                        openAudioOrVideoFiles(file, node, navController, parentCoroutineScope)
                    } else {
                        file?.let {
                            openNotStreamableFiles(
                                navController,
                                it,
                                node.type,
                                parentCoroutineScope
                            )
                        } ?: actionHandler(menuAction, node)
                    }
                }
            }
        } else {
            Timber.Forest.e("Cannot do the operation open with: Node is not a FileNode")
        }
        onDismiss()
    }

    private suspend fun openAudioOrVideoFiles(
        localFile: File?,
        node: TypedFileNode,
        navController: NavHostController,
        parentCoroutineScope: CoroutineScope,
    ) {
        val fileUri = getAudioOrVideoFileUri(localFile, node)
        Intent(Intent.ACTION_VIEW).apply {
            if (fileUri != null) {
                setDataAndType(Uri.parse(fileUri), node.type.mimeType)
            } else {
                // Todo provide snackbar
                // snackBarHandler.postSnackbarMessage(R.string.error_open_file_with)
            }
            if (resolveActivity(context.packageManager) != null) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                parentCoroutineScope.ensureActive()
                navController.context.startActivity(this)
            } else if (localFile == null) {
                parentCoroutineScope.ensureActive()
                // Todo: navigationHandler
                navController.navigate(cannotOpenFileDialog)
            } else {
                // Todo provide snackbar
                // snackBarHandler.postSnackbarMessage(R.string.intent_not_available_file)
            }
        }
    }

    private suspend fun openNotStreamableFiles(
        navController: NavHostController,
        localFile: File?,
        fileTypeInfo: FileTypeInfo,
        parentCoroutineScope: CoroutineScope,
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
                    navController.context.startActivity(this@apply)
                } else {
                    // Todo provide snackbar
                    // snackBarHandler.postSnackbarMessage(R.string.intent_not_available)
                }
            } ?: run {
                // Todo provide snackbar
                // snackBarHandler.postSnackbarMessage(R.string.general_text_error)
            }
        }
    }

    private suspend fun getAudioOrVideoFileUri(
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
    }.onFailure { Timber.Forest.e("Error getting local file uri: ${it.message}") }.getOrNull()

    private suspend fun getLocalFile(node: TypedFileNode): File? = runCatching {
        getNodePreviewFileUseCase(node)
    }.onFailure { Timber.Forest.e("Error getting local file path: ${it.message}") }.getOrNull()

    private suspend fun getStreamingUri(node: TypedFileNode) = runCatching {
        getStreamingUriStringForNode(node)
    }.onFailure { Timber.Forest.e("Error getting streaming uri: ${it.message}") }.getOrNull()

    private suspend fun startHttpServer() = runCatching {
        httpServerStartUseCase()
    }.onFailure { Timber.Forest.e("Error starting http server: ${it.message}") }.getOrNull()

    private suspend fun httpServerRunning() = runCatching {
        httpServerIsRunningUseCase()
    }.onFailure { Timber.Forest.e("Error checking if http server is running: ${it.message}") }
        .getOrDefault(0)

    override val groupId = 5

    companion object {
        // Todo duplicate to the one in mega.privacy.android.app.presentation.search.navigation.CannotOpenFileDialogNavigation.kt
        private const val cannotOpenFileDialog = "search/cannotOpenFileDialog"
    }
}