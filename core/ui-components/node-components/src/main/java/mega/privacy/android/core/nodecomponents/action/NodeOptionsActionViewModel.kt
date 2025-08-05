package mega.privacy.android.core.nodecomponents.action

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.nodecomponents.R
import mega.privacy.android.core.nodecomponents.model.NodeActionState
import mega.privacy.android.core.nodecomponents.mapper.NodeContentUriIntentMapper
import mega.privacy.android.core.nodecomponents.mapper.NodeHandlesToJsonMapper
import mega.privacy.android.core.nodecomponents.mapper.message.NodeMoveRequestMessageMapper
import mega.privacy.android.core.nodecomponents.mapper.message.NodeSendToChatMessageMapper
import mega.privacy.android.core.nodecomponents.mapper.message.NodeVersionHistoryRemoveMessageMapper
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.UrlFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.node.FileNodeContent
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.backup.BackupNodeType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.exception.NotEnoughQuotaMegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.exception.node.ForeignNodeException
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.GetFileTypeInfoByNameUseCase
import mega.privacy.android.domain.usecase.GetPathFromNodeContentUseCase
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.account.SetCopyLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.account.SetMoveLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.chat.AttachMultipleNodesUseCase
import mega.privacy.android.domain.usecase.chat.Get1On1ChatIdUseCase
import mega.privacy.android.domain.usecase.filenode.DeleteNodeVersionsUseCase
import mega.privacy.android.domain.usecase.node.CheckNodesNameCollisionUseCase
import mega.privacy.android.domain.usecase.node.CopyNodesUseCase
import mega.privacy.android.domain.usecase.node.GetNodeContentUriUseCase
import mega.privacy.android.domain.usecase.node.GetNodePreviewFileUseCase
import mega.privacy.android.domain.usecase.node.MoveNodesUseCase
import mega.privacy.android.domain.usecase.node.backup.CheckBackupNodeTypeUseCase
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Node actions view model
 *
 * @property checkNodesNameCollisionUseCase
 * @property moveNodesUseCase
 * @property copyNodesUseCase
 * @property setMoveLatestTargetPathUseCase
 * @property setCopyLatestTargetPathUseCase
 * @property deleteNodeVersionsUseCase
 * @property snackBarHandler
 * @property moveRequestMessageMapper
 * @property versionHistoryRemoveMessageMapper
 * @property checkBackupNodeTypeUseCase
 * @property attachMultipleNodesUseCase
 * @property nodeSendToChatMessageMapper
 * @property nodeHandlesToJsonMapper
 * @property getNodeContentUriUseCase
 * @property nodeContentUriIntentMapper
 * @property applicationScope
 */
@HiltViewModel
class NodeOptionsActionViewModel @Inject constructor(
    private val checkNodesNameCollisionUseCase: CheckNodesNameCollisionUseCase,
    private val moveNodesUseCase: MoveNodesUseCase,
    private val copyNodesUseCase: CopyNodesUseCase,
    private val setMoveLatestTargetPathUseCase: SetMoveLatestTargetPathUseCase,
    private val setCopyLatestTargetPathUseCase: SetCopyLatestTargetPathUseCase,
    private val deleteNodeVersionsUseCase: DeleteNodeVersionsUseCase,
    // Todo handle snackbar
    //private val snackBarHandler: SnackBarHandler,
    private val moveRequestMessageMapper: NodeMoveRequestMessageMapper,
    private val versionHistoryRemoveMessageMapper: NodeVersionHistoryRemoveMessageMapper,
    private val checkBackupNodeTypeUseCase: CheckBackupNodeTypeUseCase,
    private val attachMultipleNodesUseCase: AttachMultipleNodesUseCase,
    private val nodeSendToChatMessageMapper: NodeSendToChatMessageMapper,
    private val nodeHandlesToJsonMapper: NodeHandlesToJsonMapper,
    private val getNodeContentUriUseCase: GetNodeContentUriUseCase,
    private val nodeContentUriIntentMapper: NodeContentUriIntentMapper,
    private val getNodePreviewFileUseCase: GetNodePreviewFileUseCase,
    private val getPathFromNodeContentUseCase: GetPathFromNodeContentUseCase,
    private val updateNodeSensitiveUseCase: UpdateNodeSensitiveUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase,
    private val get1On1ChatIdUseCase: Get1On1ChatIdUseCase,
    private val getFileTypeInfoByNameUseCase: GetFileTypeInfoByNameUseCase,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {

    internal val uiState: StateFlow<NodeActionState>
        field = MutableStateFlow(NodeActionState())

    /**
     * Check move nodes name collision
     *
     * @param nodes
     * @param targetNode
     */
    fun checkNodesNameCollision(
        nodes: List<Long>,
        targetNode: Long,
        type: NodeNameCollisionType,
    ) {
        viewModelScope.launch {
            runCatching {
                checkNodesNameCollisionUseCase(
                    nodes.associateWith { targetNode },
                    type
                )
            }.onSuccess { result ->
                uiState.update { it.copy(nodeNameCollisionsResult = triggered(result)) }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Move nodes
     *
     * @param nodes
     */
    fun moveNodes(nodes: Map<Long, Long>) {
        applicationScope.launch {
            runCatching {
                moveNodesUseCase(nodes)
            }.onSuccess {
                setMoveTargetPath(nodes.values.first())
                // Todo handle snackbar
                // snackBarHandler.postSnackbarMessage(moveRequestMessageMapper(it))
            }.onFailure {
                manageCopyMoveError(it)
                Timber.e(it)
            }
        }
    }

    /**
     * Copy nodes
     *
     * @param nodes
     */
    fun copyNodes(nodes: Map<Long, Long>) {
        applicationScope.launch {
            runCatching {
                copyNodesUseCase(nodes)
            }.onSuccess {
                setCopyTargetPath(nodes.values.first())
                // Todo handle snackbar
                // snackBarHandler.postSnackbarMessage(moveRequestMessageMapper(it))
            }.onFailure {
                manageCopyMoveError(it)
                Timber.e(it)
            }
        }
    }

    private fun manageCopyMoveError(error: Throwable?) = when (error) {
        is ForeignNodeException -> uiState.update { it.copy(showForeignNodeDialog = triggered) }
        is QuotaExceededMegaException -> uiState.update {
            it.copy(showQuotaDialog = triggered(true))
        }

        is NotEnoughQuotaMegaException -> uiState.update {
            it.copy(showQuotaDialog = triggered(false))
        }

        else -> Timber.e("Error copying/moving nodes $error")
    }

    /**
     * Set last used path of move as target path for next move
     */
    private fun setMoveTargetPath(path: Long) {
        viewModelScope.launch {
            runCatching { setMoveLatestTargetPathUseCase(path) }
                .onFailure { Timber.e(it) }
        }
    }

    /**
     * Set last used path of copy as target path for next copy
     */
    private fun setCopyTargetPath(path: Long) {
        viewModelScope.launch {
            runCatching { setCopyLatestTargetPathUseCase(path) }
                .onFailure { Timber.e(it) }
        }
    }

    /**
     * Mark handle node name collision result
     */
    fun markHandleNodeNameCollisionResult() {
        uiState.update { it.copy(nodeNameCollisionsResult = consumed()) }
    }

    /**
     * Delete version history of selected node
     */
    fun deleteVersionHistory(it: Long) = applicationScope.launch {
        val result = runCatching {
            deleteNodeVersionsUseCase(NodeId(it))
        }
        versionHistoryRemoveMessageMapper(result.exceptionOrNull()).let {
            // Todo handle snackbar
            // snackBarHandler.postSnackbarMessage(it)
        }
    }

    /**
     * Mark foreign node dialog shown
     */
    fun markForeignNodeDialogShown() {
        uiState.update { it.copy(showForeignNodeDialog = consumed) }
    }

    /**
     * Mark quota dialog shown
     */
    fun markQuotaDialogShown() {
        uiState.update { it.copy(showQuotaDialog = consumed()) }
    }

    /**
     * Contact selected for folder share
     */
    fun contactSelectedForShareFolder(contactsData: List<String>, nodeHandle: List<Long>) {
        viewModelScope.launch {
            val isFromBackups = uiState.value.selectedNodes.find {
                runCatching {
                    checkBackupNodeTypeUseCase(it) != BackupNodeType.NonBackupNode
                }.getOrElse {
                    Timber.e(it)
                    false
                }
            }
            runCatching {
                nodeHandlesToJsonMapper(nodeHandle)
            }.onSuccess { handles ->
                uiState.update {
                    it.copy(
                        contactsData = triggered(
                            Triple(
                                contactsData,
                                isFromBackups != null,
                                handles
                            )
                        )
                    )
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * attach node to chat
     *
     * @param nodeHandles [LongArray] on which node is attached
     * @param chatIds [LongArray] chat ids
     */
    fun attachNodeToChats(
        nodeHandles: LongArray?,
        chatIds: LongArray?,
        userHandles: LongArray,
    ) {
        if (nodeHandles != null && chatIds != null) {
            val nodeIds = nodeHandles.map {
                NodeId(it)
            }
            viewModelScope.launch {
                // ignore the user handles that create chat failed
                val chatIdsFromUserHandles = userHandles.map { userHandle ->
                    runCatching {
                        get1On1ChatIdUseCase(userHandle)
                    }.onFailure {
                        Timber.e(it)
                    }.getOrNull()
                }.filterNotNull()
                val allChatIds = chatIdsFromUserHandles + chatIds.toList()
                val attachNodeRequest =
                    attachMultipleNodesUseCase(
                        nodeIds = nodeIds,
                        chatIds = allChatIds
                    )
                val message = nodeSendToChatMessageMapper(attachNodeRequest)
                message?.let {
                    // Todo handle snackbar
                    // snackBarHandler.postSnackbarMessage(it)
                }
            }
        }
    }

    /**
     * Contact selected for folder share
     */
    fun markShareFolderAccessDialogShown() {
        uiState.update {
            it.copy(contactsData = consumed())
        }
    }

    /**
     * Download node
     * Triggers TransferTriggerEvent.StartDownloadNode with parameter [mega.privacy.android.domain.entity.node.TypedNode]
     *
     * @param withStartMessage  Whether show start message or not.
     *                          It should be true only if the widget is not visible.
     */
    fun downloadNode(withStartMessage: Boolean) {
        uiState.value.selectedNodes.let { nodes ->
            uiState.update {
                it.copy(
                    downloadEvent = triggered(
                        TransferTriggerEvent.StartDownloadNode(
                            nodes = nodes,
                            withStartMessage = withStartMessage,
                        )
                    )
                )
            }
        }
    }

    /**
     * Download node for preview
     * Triggers TransferTriggerEvent.StartDownloadForPreview with parameter [mega.privacy.android.domain.entity.node.TypedFileNode]
     */
    fun downloadNodeForPreview(fileNode: TypedFileNode) {
        uiState.update {
            it.copy(
                downloadEvent = triggered(
                    TransferTriggerEvent.StartDownloadForPreview(
                        node = fileNode,
                        isOpenWith = false
                    )
                )
            )
        }
    }

    /**
     * Trigger download event
     */
    fun triggerDownloadEvent(
        event: TransferTriggerEvent,
    ) {
        uiState.update {
            it.copy(downloadEvent = triggered(event))
        }
    }

    /**
     * Download node for offline
     * Triggers TransferTriggerEvent.StartDownloadNode with parameter [mega.privacy.android.domain.entity.node.TypedNode]
     *
     * @param withStartMessage  Whether show start message or not.
     *                          It should be true only if the widget is not visible.
     */
    fun downloadNodeForOffline(withStartMessage: Boolean) {
        uiState.value.selectedNodes.firstOrNull().let { node ->
            uiState.update {
                it.copy(
                    downloadEvent = triggered(
                        TransferTriggerEvent.StartDownloadForOffline(
                            node = node,
                            withStartMessage = withStartMessage,
                        )
                    )
                )
            }
        }
    }

    /**
     * Download node for preview
     * Triggers TransferTriggerEvent.StartDownloadNode with parameter [mega.privacy.android.domain.entity.node.TypedNode]
     */
    fun downloadNodeForPreview(isOpenWith: Boolean) {
        uiState.value.selectedNodes.firstOrNull()?.let { node ->
            uiState.update {
                it.copy(
                    downloadEvent = triggered(
                        TransferTriggerEvent.StartDownloadForPreview(
                            node = node,
                            isOpenWith = isOpenWith
                        )
                    )
                )
            }
        }
    }

    /**
     * Mark download event consumed
     */
    fun markDownloadEventConsumed() {
        uiState.update {
            it.copy(downloadEvent = consumed())
        }
    }

    /**
     * Update selected nodes
     * @param selectedNodes
     */
    fun updateSelectedNodes(selectedNodes: List<TypedNode>) {
        uiState.update {
            it.copy(selectedNodes = selectedNodes)
        }
    }

    /**
     * Update select All
     */
    fun selectAllClicked() {
        uiState.update {
            it.copy(selectAll = triggered)
        }
    }

    /**
     * Consume select All
     */
    fun selectAllConsumed() {
        uiState.update {
            it.copy(selectAll = consumed)
        }
    }

    /**
     * Update clear All
     */
    fun clearAllClicked() {
        uiState.update {
            it.copy(clearAll = triggered)
        }
    }

    /**
     * Consume clear All
     */
    fun clearAllConsumed() {
        uiState.update {
            it.copy(clearAll = consumed)
        }
    }

    /**
     * Consumes the event of showing info.
     */
    fun onInfoToShowEventConsumed() {
        uiState.update { state -> state.copy(infoToShowEvent = consumed()) }
    }

    /**
     * Handle file node clicked
     *
     * @param fileNode
     */
    suspend fun handleFileNodeClicked(fileNode: TypedFileNode) = when {
        fileNode.type is PdfFileTypeInfo -> FileNodeContent.Pdf(
            uri = getNodeContentUriUseCase(fileNode)
        )

        fileNode.type is ImageFileTypeInfo -> FileNodeContent.ImageForNode

        fileNode.type is TextFileTypeInfo && fileNode.size <= TextFileTypeInfo.Companion.MAX_SIZE_OPENABLE_TEXT_FILE -> FileNodeContent.TextContent

        fileNode.type is VideoFileTypeInfo || fileNode.type is AudioFileTypeInfo -> {
            FileNodeContent.AudioOrVideo(
                uri = getNodeContentUriUseCase(fileNode)
            )
        }

        fileNode.type is UrlFileTypeInfo -> {
            val content = getNodeContentUriUseCase(fileNode)
            val path = getPathFromNodeContentUseCase(content)
            FileNodeContent.UrlContent(
                uri = content,
                path = path
            )
        }

        else -> FileNodeContent.Other(
            localFile = getNodePreviewFileUseCase(fileNode)
        )
    }

    /**
     * Apply node content uri
     *
     * @param intent
     * @param content
     * @param mimeType
     * @param isSupported
     */
    fun applyNodeContentUri(
        intent: Intent,
        content: NodeContentUri,
        mimeType: String,
        isSupported: Boolean = true,
    ) {
        nodeContentUriIntentMapper(intent, content, mimeType, isSupported)
    }

    fun handleHiddenNodesOnboardingResult(isOnboarded: Boolean) {
        viewModelScope.launch {
            runCatching {
                if (isOnboarded) {
                    val selectedNodes = uiState.value.selectedNodes

                    selectedNodes.forEach {
                        updateNodeSensitiveUseCase(
                            nodeId = it.id,
                            isSensitive = true,
                        )
                    }
                    uiState.update { state ->
                        state.copy(
                            infoToShowEvent = triggered(
                                LocalizedText.PluralsRes(
                                    resId = R.plurals.hidden_nodes_result_message,
                                    quantity = selectedNodes.size,
                                )
                            )
                        )
                    }
                }
            }.onFailure { Timber.e(it) }
        }
    }

    suspend fun isOnboarding(): Boolean {
        val accountType =
            monitorAccountDetailUseCase().first().levelDetail?.accountType
        val isPaid = accountType?.isPaid == true
        val businessStatus =
            if (isPaid && accountType.isBusinessAccount == true) {
                getBusinessStatusUseCase()
            } else null
        val isBusinessAccountExpired = businessStatus == BusinessAccountStatus.Expired
        return isPaid && !isBusinessAccountExpired
    }

    /**
     * return the file type of the given file
     */
    fun getTypeInfo(file: File) = getFileTypeInfoByNameUseCase(file.name)
}