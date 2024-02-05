package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.navigation.NavHostController
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.node.model.menuaction.OpenWithMenuAction
import mega.privacy.android.app.presentation.search.navigation.cannotOpenFileDialog
import mega.privacy.android.app.presentation.snackbar.SnackBarHandler
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.file.GetFileUriUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import mega.privacy.android.domain.usecase.node.GetNodePreviewFilePathUseCase
import mega.privacy.android.domain.usecase.streaming.GetStreamingUriStringForNode
import timber.log.Timber
import java.io.File
import javax.inject.Inject


/**
 * Open with bottom sheet menu item
 *
 * @param menuAction [OpenWithMenuAction]
 * @param getFileUriUseCase [GetFileUriUseCase]
 * @param getLocalFilePathUseCase [GetNodePreviewFilePathUseCase]
 * @param httpServerStartUseCase [MegaApiHttpServerStartUseCase]
 * @param getStreamingUriStringForNode [GetStreamingUriStringForNode]
 * @param snackBarHandler [SnackBarHandler]
 * @param context [Context]
 * @param scope [CoroutineScope]
 */
class OpenWithBottomSheetMenuItem @Inject constructor(
    override val menuAction: OpenWithMenuAction,
    private val getFileUriUseCase: GetFileUriUseCase,
    private val getLocalFilePathUseCase: GetNodePreviewFilePathUseCase,
    private val httpServerStartUseCase: MegaApiHttpServerStartUseCase,
    private val httpServerIsRunningUseCase: MegaApiHttpServerIsRunningUseCase,
    private val getStreamingUriStringForNode: GetStreamingUriStringForNode,
    private val snackBarHandler: SnackBarHandler,
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
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
        actionHandler: (menuAction: MenuAction, node: TypedNode) -> Unit,
        navController: NavHostController,
    ): () -> Unit = {
        if (node is TypedFileNode) {
            scope.launch {
                val localPath = getLocalFilePath(node)
                if (node.type is AudioFileTypeInfo || node.type is VideoFileTypeInfo) {
                    openAudioOrVideoFiles(localPath, node, navController)
                } else {
                    localPath?.let {
                        openNotStreamableFiles(navController, it, node.type)
                    } ?: actionHandler(menuAction, node)
                }
            }
        } else {
            Timber.e("Cannot do the operation open with: Node is not a FileNode")
        }
        onDismiss()
    }

    private suspend fun openAudioOrVideoFiles(
        localPath: String?,
        node: TypedFileNode,
        navController: NavHostController,
    ) {
        val fileUri = getAudioOrVideoFileUri(localPath, node)
        Intent(Intent.ACTION_VIEW).apply {
            if (fileUri != null) {
                setDataAndType(Uri.parse(fileUri), node.type.mimeType)
            } else {
                snackBarHandler.postSnackbarMessage(R.string.error_open_file_with)
            }
            if (resolveActivity(context.packageManager) != null) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                navController.context.startActivity(this)
            } else if (localPath == null) {
                navController.navigate(cannotOpenFileDialog)
            } else {
                snackBarHandler.postSnackbarMessage(R.string.intent_not_available_file)
            }
        }
    }

    private suspend fun openNotStreamableFiles(
        navController: NavHostController,
        localPath: String,
        fileTypeInfo: FileTypeInfo,
    ) {
        val localFileUri = getLocalFileUri(localPath)
        Intent(Intent.ACTION_VIEW).apply {
            localFileUri?.let {
                setDataAndType(Uri.parse(it), fileTypeInfo.mimeType)
                if (resolveActivity(context.packageManager) == null) {
                    action = Intent.ACTION_SEND
                }
                if (resolveActivity(context.packageManager) != null) {
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    navController.context.startActivity(this@apply)
                } else {
                    snackBarHandler.postSnackbarMessage(R.string.intent_not_available)
                }
            } ?: snackBarHandler.postSnackbarMessage(R.string.general_text_error)
        }
    }

    private suspend fun getAudioOrVideoFileUri(
        localPath: String?,
        node: TypedFileNode,
    ): String? = localPath?.let {
        getLocalFileUri(it)
    } ?: run {
        if (httpServerRunning() == 0) {
            startHttpServer()
        }
        getStreamingUri(node)
    }

    private suspend fun getLocalFileUri(filePath: String) = runCatching {
        getFileUriUseCase(File(filePath), Constants.AUTHORITY_STRING_FILE_PROVIDER)
    }.onFailure { Timber.e("Error getting local file uri: ${it.message}") }.getOrNull()

    private suspend fun getLocalFilePath(node: TypedFileNode): String? = runCatching {
        getLocalFilePathUseCase(node)
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

    override val groupId = 5
}