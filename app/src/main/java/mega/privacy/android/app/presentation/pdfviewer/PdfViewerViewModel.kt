package mega.privacy.android.app.presentation.pdfviewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.domain.usecase.CheckNameCollision
import mega.privacy.android.app.namecollision.data.NameCollisionType
import mega.privacy.android.app.namecollision.usecase.CheckNameCollisionUseCase
import mega.privacy.android.app.usecase.exception.MegaNodeException
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.file.GetDataBytesFromUrlUseCase
import mega.privacy.android.domain.usecase.node.CopyChatNodeUseCase
import mega.privacy.android.domain.usecase.node.CopyNodeUseCase
import mega.privacy.android.domain.usecase.node.MoveNodeUseCase
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.net.URL
import javax.inject.Inject

/**
 * view model for [PdfViewerActivity]
 *
 * @property moveNodeUseCase        [MoveNodeUseCase]
 */
@HiltViewModel
class PdfViewerViewModel @Inject constructor(
    private val copyNodeUseCase: CopyNodeUseCase,
    private val moveNodeUseCase: MoveNodeUseCase,
    private val checkNameCollision: CheckNameCollision,
    private val checkNameCollisionUseCase: CheckNameCollisionUseCase,
    private val getDataBytesFromUrlUseCase: GetDataBytesFromUrlUseCase,
    private val updateNodeSensitiveUseCase: UpdateNodeSensitiveUseCase,
    private val copyChatNodeUseCase: CopyChatNodeUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val isHiddenNodesOnboardedUseCase: IsHiddenNodesOnboardedUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(PdfViewerState())

    /**
     * UI State PdfViewer
     * Flow of [PdfViewerState]
     */
    val uiState = _state.asStateFlow()

    init {
        monitorAccountDetail()
        monitorIsHiddenNodesOnboarded()
    }

    /**
     * Imports a chat node if there is no name collision.
     *
     * @param node              Node handle to copy.
     * @param chatId            Chat ID where the node is.
     * @param messageId         Message ID where the node is.
     * @param newParentHandle   Parent handle in which the node will be copied.
     */
    fun importChatNode(
        node: MegaNode,
        chatId: Long,
        messageId: Long,
        newParentHandle: NodeId,
    ) = viewModelScope.launch {
        runCatching {
            checkNameCollisionUseCase.check(
                node = node,
                parentHandle = newParentHandle.longValue,
                type = NameCollisionType.COPY,
            )
        }.onSuccess { collisionResult ->
            _state.update { it.copy(nameCollision = collisionResult) }
        }.onFailure { throwable ->
            when (throwable) {
                is MegaNodeException.ChildDoesNotExistsException -> {
                    copyChatNode(chatId, messageId, newParentHandle)
                }

                else -> Timber.e(throwable)
            }
        }
    }

    /**
     * Copies a chat node
     * @param chatId Chat ID where the node is.
     * @param messageId Message ID where the node is.
     * @param newParentNodeId Parent handle in which the node will be copied.
     */
    private fun copyChatNode(chatId: Long, messageId: Long, newParentNodeId: NodeId) {
        viewModelScope.launch {
            runCatching {
                copyChatNodeUseCase(
                    chatId = chatId,
                    messageId = messageId,
                    newNodeParent = newParentNodeId,
                )
            }.onSuccess {
                _state.update {
                    it.copy(snackBarMessage = R.string.context_correctly_copied)
                }
            }.onFailure { throwable ->
                Timber.e(throwable, "The chat node is not copied")
                _state.update {
                    it.copy(nodeCopyError = throwable)
                }
            }
        }
    }

    /**
     * Copies a node if there is no name collision.
     *
     * @param nodeHandle        Node handle to copy.
     * @param newParentHandle   Parent handle in which the node will be copied.
     */
    fun copyNode(nodeHandle: Long, newParentHandle: Long) {
        viewModelScope.launch {
            checkForNameCollision(
                nodeHandle = nodeHandle,
                newParentHandle = newParentHandle,
                type = NameCollisionType.COPY
            ) {
                runCatching {
                    copyNodeUseCase(
                        nodeToCopy = NodeId(nodeHandle),
                        newNodeParent = NodeId(newParentHandle),
                        newNodeName = null,
                    )
                }.onSuccess {
                    _state.update {
                        it.copy(snackBarMessage = R.string.context_correctly_copied)
                    }
                }.onFailure { error ->
                    _state.update {
                        it.copy(nodeCopyError = error)
                    }
                    Timber.e("Error not copied $error")
                }
            }
        }
    }

    /**
     * Moves a node if there is no name collision.
     *
     * @param nodeHandle        Node handle to move.
     * @param newParentHandle   Parent handle in which the node will be moved.
     */
    fun moveNode(nodeHandle: Long, newParentHandle: Long) {
        viewModelScope.launch {
            checkForNameCollision(
                nodeHandle = nodeHandle,
                newParentHandle = newParentHandle,
                type = NameCollisionType.MOVE
            ) {
                viewModelScope.launch {
                    runCatching {
                        moveNodeUseCase(
                            nodeToMove = NodeId(nodeHandle),
                            newNodeParent = NodeId(newParentHandle),
                        )
                    }.onSuccess {
                        _state.update {
                            it.copy(
                                snackBarMessage = R.string.context_correctly_moved,
                                shouldFinishActivity = true
                            )
                        }
                    }.onFailure { error ->
                        _state.update {
                            it.copy(nodeMoveError = error)
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks if there is a name collision before proceeding with the action.
     *
     * @param nodeHandle        Handle of the node to check the name collision.
     * @param newParentHandle   Handle of the parent folder in which the action will be performed.
     * @param completeAction    Action to complete after checking the name collision.
     */
    private suspend fun checkForNameCollision(
        nodeHandle: Long,
        newParentHandle: Long,
        type: NameCollisionType,
        completeAction: suspend (() -> Unit),
    ) {
        runCatching {
            checkNameCollision(
                nodeHandle = NodeId(nodeHandle),
                parentHandle = NodeId(newParentHandle),
                type = type,
            )
        }.onSuccess { collision ->
            _state.update { it.copy(nameCollision = collision) }
        }.onFailure {
            when (it) {
                is MegaNodeException.ChildDoesNotExistsException -> completeAction.invoke()
                is MegaNodeException.ParentDoesNotExistException -> _state.update { state ->
                    state.copy(
                        snackBarMessage = R.string.general_error
                    )
                }

                else -> Timber.e(it)
            }
        }
    }

    /**
     * Load pdf stream data from url
     */
    fun loadPdfStream(uri: String) {
        viewModelScope.launch {
            runCatching {
                getDataBytesFromUrlUseCase(URL(uri))
            }.onSuccess { data ->
                _state.update { it.copy(pdfStreamData = data) }
            }.onFailure { Timber.e("Exception loading PDF as stream", it) }
        }
    }

    /**
     * onConsumeSnackBarMessage
     *
     * resets SnackBar state to null once SnackBar is shown
     */
    fun onConsumeSnackBarMessage() {
        _state.update { it.copy(snackBarMessage = null) }
    }

    /**
     * onConsume Copy Error
     *
     * resets throwable to null once error is displayed to user
     */
    fun onConsumeNodeMoveError() {
        _state.update { it.copy(nodeMoveError = null) }
    }

    /**
     * onConsume Copy Error
     *
     * resets throwable to null once error is displayed to user
     */
    fun onConsumeNodeCopyError() {
        _state.update { it.copy(nodeCopyError = null) }
    }

    /**
     * Reset pdf stream data
     */
    fun resetPdfStreamData() {
        _state.update { it.copy(pdfStreamData = null) }
    }

    /**
     * Hide or unhide the node by modifying the sensitive attribute
     */
    fun hideOrUnhideNode(nodeId: NodeId, hide: Boolean) = viewModelScope.launch {
        updateNodeSensitiveUseCase(nodeId = nodeId, isSensitive = hide)
    }

    private fun monitorAccountDetail() {
        monitorAccountDetailUseCase()
            .onEach { accountDetail ->
                _state.update {
                    it.copy(accountType = accountDetail.levelDetail?.accountType)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun monitorIsHiddenNodesOnboarded() {
        viewModelScope.launch {
            val isHiddenNodesOnboarded = isHiddenNodesOnboardedUseCase()
            _state.update {
                it.copy(isHiddenNodesOnboarded = isHiddenNodesOnboarded)
            }
        }
    }

    fun setHiddenNodesOnboarded() {
        _state.update {
            it.copy(isHiddenNodesOnboarded = true)
        }
    }
}