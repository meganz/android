package mega.privacy.android.app.presentation.pdfviewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.domain.usecase.CheckNameCollision
import mega.privacy.android.app.namecollision.data.NameCollisionType
import mega.privacy.android.app.usecase.exception.MegaNodeException
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.node.CopyNodeUseCase
import mega.privacy.android.domain.usecase.node.MoveNodeUseCase
import timber.log.Timber
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
) : ViewModel() {

    private val _state = MutableStateFlow(PdfViewerState())

    /**
     * UI State PdfViewer
     * Flow of [PdfViewerState]
     */
    val uiState = _state.asStateFlow()

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
                Timber.d("SMP move node Imager view model")
                viewModelScope.launch {
                    runCatching {
                        moveNodeUseCase(
                            nodeToMove = NodeId(nodeHandle),
                            newNodeParent = NodeId(newParentHandle),
                        )
                    }.onSuccess {
                        Timber.d("SMP move node Imager view model success")
                        _state.update {
                            it.copy(
                                snackBarMessage = R.string.context_correctly_moved,
                                shouldFinishActivity = true
                            )
                        }
                    }.onFailure { error ->
                        Timber.d("SMP move node Imager view model failure")
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
}