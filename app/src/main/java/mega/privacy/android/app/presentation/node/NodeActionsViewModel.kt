package mega.privacy.android.app.presentation.node

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
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.chat.mapper.ChatRequestMessageMapper
import mega.privacy.android.app.presentation.meeting.chat.model.InfoToShow
import mega.privacy.android.app.presentation.meeting.chat.view.message.attachment.NodeContentUriIntentMapper
import mega.privacy.android.app.presentation.movenode.mapper.MoveRequestMessageMapper
import mega.privacy.android.app.presentation.node.model.NodeActionState
import mega.privacy.android.app.presentation.snackbar.SnackBarHandler
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.app.presentation.versions.mapper.VersionHistoryRemoveMessageMapper
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.UrlFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeContentUri
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.backup.BackupNodeType
import mega.privacy.android.domain.exception.NotEnoughQuotaMegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.exception.node.ForeignNodeException
import mega.privacy.android.domain.qualifier.ApplicationScope
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
import mega.privacy.android.domain.usecase.node.backup.CheckBackupNodeTypeByHandleUseCase
import mega.privacy.android.feature.sync.data.mapper.ListToStringWithDelimitersMapper
import timber.log.Timber
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
 * @property checkBackupNodeTypeByHandleUseCase
 * @property attachMultipleNodesUseCase
 * @property chatRequestMessageMapper
 * @property listToStringWithDelimitersMapper
 * @property getNodeContentUriUseCase
 * @property nodeContentUriIntentMapper
 * @property applicationScope
 */
@HiltViewModel
class NodeActionsViewModel @Inject constructor(
    private val checkNodesNameCollisionUseCase: CheckNodesNameCollisionUseCase,
    private val moveNodesUseCase: MoveNodesUseCase,
    private val copyNodesUseCase: CopyNodesUseCase,
    private val setMoveLatestTargetPathUseCase: SetMoveLatestTargetPathUseCase,
    private val setCopyLatestTargetPathUseCase: SetCopyLatestTargetPathUseCase,
    private val deleteNodeVersionsUseCase: DeleteNodeVersionsUseCase,
    private val snackBarHandler: SnackBarHandler,
    private val moveRequestMessageMapper: MoveRequestMessageMapper,
    private val versionHistoryRemoveMessageMapper: VersionHistoryRemoveMessageMapper,
    private val checkBackupNodeTypeByHandleUseCase: CheckBackupNodeTypeByHandleUseCase,
    private val attachMultipleNodesUseCase: AttachMultipleNodesUseCase,
    private val chatRequestMessageMapper: ChatRequestMessageMapper,
    private val listToStringWithDelimitersMapper: ListToStringWithDelimitersMapper,
    private val getNodeContentUriUseCase: GetNodeContentUriUseCase,
    private val nodeContentUriIntentMapper: NodeContentUriIntentMapper,
    private val getNodePreviewFileUseCase: GetNodePreviewFileUseCase,
    private val getPathFromNodeContentUseCase: GetPathFromNodeContentUseCase,
    private val updateNodeSensitiveUseCase: UpdateNodeSensitiveUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val get1On1ChatIdUseCase: Get1On1ChatIdUseCase,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {

    private val _state = MutableStateFlow(NodeActionState())

    /**
     * public UI State
     */
    val state: StateFlow<NodeActionState> = _state

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
                _state.update { it.copy(nodeNameCollisionsResult = triggered(result)) }
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
                snackBarHandler.postSnackbarMessage(moveRequestMessageMapper(it))
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
                snackBarHandler.postSnackbarMessage(moveRequestMessageMapper(it))
            }.onFailure {
                manageCopyMoveError(it)
                Timber.e(it)
            }
        }
    }

    private fun manageCopyMoveError(error: Throwable?) = when (error) {
        is ForeignNodeException -> _state.update { it.copy(showForeignNodeDialog = triggered) }
        is QuotaExceededMegaException -> _state.update {
            it.copy(showQuotaDialog = triggered(true))
        }

        is NotEnoughQuotaMegaException -> _state.update {
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
        _state.update { it.copy(nodeNameCollisionsResult = consumed()) }
    }

    /**
     * Delete version history of selected node
     */
    fun deleteVersionHistory(it: Long) = applicationScope.launch {
        val result = runCatching {
            deleteNodeVersionsUseCase(NodeId(it))
        }
        versionHistoryRemoveMessageMapper(result.exceptionOrNull()).let {
            snackBarHandler.postSnackbarMessage(it)
        }
    }

    /**
     * Mark foreign node dialog shown
     */
    fun markForeignNodeDialogShown() {
        _state.update { it.copy(showForeignNodeDialog = consumed) }
    }

    /**
     * Mark quota dialog shown
     */
    fun markQuotaDialogShown() {
        _state.update { it.copy(showQuotaDialog = consumed()) }
    }

    /**
     * Contact selected for folder share
     */
    fun contactSelectedForShareFolder(contactsData: List<String>, nodeHandle: List<Long>) {
        viewModelScope.launch {
            val isFromBackups = state.value.selectedNodes.find {
                runCatching {
                    checkBackupNodeTypeByHandleUseCase(it) != BackupNodeType.NonBackupNode
                }.getOrElse {
                    Timber.e(it)
                    false
                }
            }
            runCatching {
                listToStringWithDelimitersMapper(nodeHandle)
            }.onSuccess { handles ->
                _state.update {
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
                val message = chatRequestMessageMapper(attachNodeRequest)
                message?.let {
                    snackBarHandler.postSnackbarMessage(it)
                }
            }
        }
    }

    /**
     * Contact selected for folder share
     */
    fun markShareFolderAccessDialogShown() {
        _state.update {
            it.copy(contactsData = consumed())
        }
    }

    /**
     * Download node
     * Triggers TransferTriggerEvent.StartDownloadNode with parameter [TypedNode]
     */
    fun downloadNode() {
        state.value.selectedNodes.let { nodes ->
            _state.update {
                it.copy(downloadEvent = triggered(TransferTriggerEvent.StartDownloadNode(nodes)))
            }
        }
    }

    /**
     * Download node for preview
     * Triggers TransferTriggerEvent.StartDownloadForPreview with parameter [TypedFileNode]
     */
    fun downloadNodeForPreview(fileNode: TypedFileNode) {
        _state.update {
            it.copy(downloadEvent = triggered(TransferTriggerEvent.StartDownloadForPreview(fileNode)))
        }
    }

    /**
     * Download node for offline
     * Triggers TransferTriggerEvent.StartDownloadNode with parameter [TypedNode]
     */
    fun downloadNodeForOffline() {
        state.value.selectedNodes.firstOrNull().let { node ->
            _state.update {
                it.copy(downloadEvent = triggered(TransferTriggerEvent.StartDownloadForOffline(node)))
            }
        }
    }

    /**
     * Download node for preview
     * Triggers TransferTriggerEvent.StartDownloadNode with parameter [TypedNode]
     */
    fun downloadNodeForPreview() {
        state.value.selectedNodes.firstOrNull()?.let { node ->
            _state.update {
                it.copy(downloadEvent = triggered(TransferTriggerEvent.StartDownloadForPreview(node)))
            }
        }
    }

    /**
     * Mark download event consumed
     */
    fun markDownloadEventConsumed() {
        _state.update {
            it.copy(downloadEvent = consumed())
        }
    }

    /**
     * Update selected nodes
     * @param selectedNodes
     */
    fun updateSelectedNodes(selectedNodes: List<TypedNode>) {
        _state.update {
            it.copy(selectedNodes = selectedNodes)
        }
    }

    /**
     * Update select All
     */
    fun selectAllClicked() {
        _state.update {
            it.copy(selectAll = triggered)
        }
    }

    /**
     * Consume select All
     */
    fun selectAllConsumed() {
        _state.update {
            it.copy(selectAll = consumed)
        }
    }

    /**
     * Update clear All
     */
    fun clearAllClicked() {
        _state.update {
            it.copy(clearAll = triggered)
        }
    }

    /**
     * Consume clear All
     */
    fun clearAllConsumed() {
        _state.update {
            it.copy(clearAll = consumed)
        }
    }

    /**
     * Consumes the event of showing info.
     */
    fun onInfoToShowEventConsumed() {
        _state.update { state -> state.copy(infoToShowEvent = consumed()) }
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

        fileNode.type is TextFileTypeInfo && fileNode.size <= TextFileTypeInfo.MAX_SIZE_OPENABLE_TEXT_FILE -> FileNodeContent.TextContent

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
                    val selectedNodes = _state.value.selectedNodes

                    selectedNodes.forEach {
                        updateNodeSensitiveUseCase(
                            nodeId = it.id,
                            isSensitive = true,
                        )
                    }
                    _state.update { state ->
                        state.copy(
                            infoToShowEvent = triggered(
                                InfoToShow.QuantityString(
                                    stringId = R.plurals.hidden_nodes_result_message,
                                    count = selectedNodes.size,
                                )
                            )
                        )
                    }
                }
            }.onFailure { Timber.e(it) }
        }
    }

    suspend fun isOnboarding() =
        monitorAccountDetailUseCase().first().levelDetail?.accountType?.isPaid ?: false
}