package mega.privacy.android.core.nodecomponents.action

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
import mega.privacy.android.core.nodecomponents.dialog.delete.MoveToRubbishOrDeleteDialogArgs
import mega.privacy.android.core.nodecomponents.mapper.NodeHandlesToJsonMapper
import mega.privacy.android.core.nodecomponents.mapper.RestoreNodeResultMapper
import mega.privacy.android.core.nodecomponents.menu.menuaction.AvailableOfflineMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.CopyMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.DeletePermanentlyMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.DownloadMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.HideMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.ManageLinkMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.MoveMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.OpenWithMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.RenameMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.RestoreMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.SendToChatMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.ShareFolderMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.TrashMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.VersionsMenuAction
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.chat.GetNodeToAttachUseCase
import mega.privacy.android.domain.usecase.file.GetFileUriUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import mega.privacy.android.domain.usecase.node.CheckNodesNameCollisionUseCase
import mega.privacy.android.domain.usecase.node.GetNodePreviewFileUseCase
import mega.privacy.android.domain.usecase.node.RestoreNodesUseCase
import mega.privacy.android.domain.usecase.offline.RemoveOfflineNodeUseCase
import mega.privacy.android.domain.usecase.streaming.GetStreamingUriStringForNode
import mega.privacy.android.shared.resources.R as sharedResR
import timber.log.Timber
import java.io.File
import javax.inject.Inject

interface BaseNodeAction {
    fun canHandle(action: MenuAction): Boolean
}

/**
 * Handler for single node operations.
 */
interface SingleNodeAction : BaseNodeAction {
    fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider)
}

/**
 * Handler for multiple nodes operations.
 */
interface MultiNodeAction : BaseNodeAction {
    fun handle(action: MenuAction, nodes: List<TypedNode>, provider: MultipleNodesActionProvider)
}

class VersionsAction @Inject constructor() : SingleNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is VersionsMenuAction
    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        provider.versionsLauncher.launch(node.id.longValue)
    }
}

class MoveAction @Inject constructor() : SingleNodeAction, MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is MoveMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        provider.moveLauncher.launch(longArrayOf(node.id.longValue))
    }

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        val nodeHandleArray = nodes.map { it.id.longValue }.toLongArray()
        provider.moveLauncher.launch(nodeHandleArray)
    }
}

class CopyAction @Inject constructor() : SingleNodeAction, MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is CopyMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        provider.copyLauncher.launch(longArrayOf(node.id.longValue))
    }

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        val nodeHandleArray = nodes.map { it.id.longValue }.toLongArray()
        provider.copyLauncher.launch(nodeHandleArray)
    }
}

class ShareFolderAction @Inject constructor() : SingleNodeAction, MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is ShareFolderMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        provider.viewModel.verifyShareFolderAction(node)
    }

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        provider.viewModel.verifyShareFolderAction(nodes)
    }
}

class RestoreAction @Inject constructor(
    private val checkNodesNameCollisionUseCase: CheckNodesNameCollisionUseCase,
    private val restoreNodesUseCase: RestoreNodesUseCase,
    private val restoreNodeResultMapper: RestoreNodeResultMapper,
) : SingleNodeAction, MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is RestoreMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        handleRestore(listOf(node), provider)
    }

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        handleRestore(nodes, provider)
    }

    private fun handleRestore(
        nodes: List<TypedNode>,
        provider: NodeActionProvider,
    ) {
        provider.coroutineScope.launch {
            withContext(NonCancellable) {
                val restoreMap = nodes.associate { node ->
                    node.id.longValue to (node.restoreId?.longValue ?: -1L)
                }
                runCatching {
                    checkNodesNameCollisionUseCase(restoreMap, NodeNameCollisionType.RESTORE)
                }.onSuccess { result ->
                    if (result.conflictNodes.isNotEmpty()) {
                        provider.coroutineScope.ensureActive()
                        val nodeHandleArray = nodes.map { it.id.longValue }.toLongArray()
                        provider.restoreLauncher.launch(nodeHandleArray)
                    }
                    if (result.noConflictNodes.isNotEmpty()) {
                        val restoreResult = restoreNodesUseCase(result.noConflictNodes)
                        val message = restoreNodeResultMapper(restoreResult)
                        provider.postMessage(message)
                    }
                }.onFailure { throwable ->
                    Timber.e(throwable)
                }
            }
        }
    }
}

class SendToChatAction @Inject constructor(
    private val getNodeToAttachUseCase: GetNodeToAttachUseCase,
) : SingleNodeAction, MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is SendToChatMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        if (node is TypedFileNode) {
            provider.coroutineScope.launch {
                runCatching {
                    getNodeToAttachUseCase(node)
                }.onSuccess { typedNode ->
                    typedNode?.let {
                        provider.sendToChatLauncher.launch(
                            longArrayOf(node.id.longValue)
                        )
                    }
                }.onFailure { Timber.e(it) }
            }
        }
    }

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        val nodeHandleArray = nodes.map { it.id.longValue }.toLongArray()
        provider.sendToChatLauncher.launch(nodeHandleArray)
    }
}

class OpenWithAction @Inject constructor(
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

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        provider.viewModel.downloadNodeForPreview(true)
    }
}

class DownloadAction @Inject constructor() : SingleNodeAction, MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is DownloadMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        provider.viewModel.downloadNode(withStartMessage = false)
    }

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        provider.viewModel.downloadNode(withStartMessage = false)
    }
}

class AvailableOfflineAction @Inject constructor(
    private val removeOfflineNodeUseCase: RemoveOfflineNodeUseCase,
) : SingleNodeAction, MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is AvailableOfflineMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        if (node.isAvailableOffline) {
            provider.coroutineScope.launch {
                withContext(NonCancellable) {
                    runCatching {
                        removeOfflineNodeUseCase(nodeId = node.id)
                    }.onFailure { Timber.Forest.e(it) }
                }
            }
        } else {
            provider.viewModel.downloadNodeForOffline(withStartMessage = false)
        }
    }

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        // Assumes that all selected nodes have the same offline status
        provider.viewModel.downloadNodeForOffline(withStartMessage = false)
    }
}

class HideAction @Inject constructor(
    private val isHiddenNodesOnboardedUseCase: IsHiddenNodesOnboardedUseCase,
) : SingleNodeAction, MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is HideMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        // Todo handle analytics tracking
        //Analytics.tracker.trackEvent(HideNodeMenuItemEvent)
        handleHide(provider)
    }

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        // Todo add analytics when available
        //Analytics.tracker.trackEvent(HideNodeMultiSelectMenuItemEvent)
        handleHide(provider)
    }

    private fun handleHide(provider: NodeActionProvider) {
        provider.coroutineScope.launch {
            val isHiddenNodesOnboarded = isHiddenNodesOnboardedUseCase()
            val isOnboarding = provider.viewModel.isOnboarding()
            if (isOnboarding && isHiddenNodesOnboarded) {
                provider.viewModel.handleHiddenNodesOnboardingResult(
                    isOnboarded = true,
                    isHidden = true
                )
            } else {
                provider.hiddenNodesOnboardingLauncher.launch(isOnboarding)
            }
        }
    }
}

class RenameNodeAction @Inject constructor() : SingleNodeAction {
    override fun canHandle(action: MenuAction): Boolean =
        action is RenameMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        provider.viewModel.handleRenameNodeRequest(node.id)
    }
}

class MoveToRubbishBinAction @Inject constructor(
    private val nodeHandlesToJsonMapper: NodeHandlesToJsonMapper,
) : SingleNodeAction, MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is TrashMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        handleTrashAction(listOf(node), provider)
    }

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        handleTrashAction(nodes, provider)
    }

    private fun handleTrashAction(
        nodes: List<TypedNode>,
        provider: NodeActionProvider,
    ) {
        provider.coroutineScope.launch {
            val handles = nodes.map { it.id.longValue }
            runCatching { nodeHandlesToJsonMapper(handles) }
                .onSuccess {
                    provider.navigationHandler?.navigate(
                        MoveToRubbishOrDeleteDialogArgs(
                            isInRubbish = false,
                            nodeHandles = handles
                        )
                    )
                }
                .onFailure { Timber.e(it) }
        }
    }
}

class ManageLinkAction @Inject constructor() : SingleNodeAction, MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is ManageLinkMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        provider.megaNavigator.openGetLinkActivity(
            context = provider.context,
            handle = node.id.longValue
        )
    }

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        val handles = nodes.map { it.id.longValue }.toLongArray()
        provider.megaNavigator.openGetLinkActivity(
            context = provider.context,
            handles = handles
        )
    }
}

class DeletePermanentAction @Inject constructor(
    private val nodeHandlesToJsonMapper: NodeHandlesToJsonMapper,
) : SingleNodeAction, MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is DeletePermanentlyMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        deleteNodes(listOf(node), provider)
    }

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        deleteNodes(nodes, provider)
    }

    private fun deleteNodes(
        nodes: List<TypedNode>,
        provider: NodeActionProvider,
    ) {
        provider.coroutineScope.launch {
            withContext(NonCancellable) {
                val handles = nodes.map { it.id.longValue }
                runCatching { nodeHandlesToJsonMapper(handles) }
                    .onSuccess {
                        provider.navigationHandler?.navigate(
                            MoveToRubbishOrDeleteDialogArgs(
                                isInRubbish = true,
                                nodeHandles = handles
                            )
                        )
                    }
                    .onFailure { Timber.e(it) }
            }
        }
    }
}