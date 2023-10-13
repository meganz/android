package mega.privacy.android.app.presentation.contact

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.node.NodeNameCollisionResult
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.node.CheckNodesNameCollisionUseCase
import mega.privacy.android.domain.usecase.node.CopyNodesUseCase
import mega.privacy.android.domain.usecase.node.MoveNodesToRubbishUseCase
import mega.privacy.android.domain.usecase.node.MoveNodesUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * View Model for [mega.privacy.android.app.main.ContactFileListActivity]
 */
@HiltViewModel
class ContactFileListViewModel @Inject constructor(
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
    private val moveNodesToRubbishUseCase: MoveNodesToRubbishUseCase,
    private val checkNodesNameCollisionUseCase: CheckNodesNameCollisionUseCase,
    private val moveNodesUseCase: MoveNodesUseCase,
    private val copyNodesUseCase: CopyNodesUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(ContactFileListUiState())

    /**
     * Ui State
     */
    val state = _state.asStateFlow()

    /**
     * Get latest [StorageState] from [MonitorStorageStateEventUseCase] use case.
     * @return the latest [StorageState]
     */
    fun getStorageState(): StorageState = monitorStorageStateEventUseCase.getState()

    /**
     * Is online
     *
     * @return
     */
    fun isOnline(): Boolean = isConnectedToInternetUseCase()

    /**
     * Move nodes to rubbish
     *
     * @param nodeHandles
     */
    fun moveNodesToRubbish(nodeHandles: List<Long>) {
        viewModelScope.launch {
            val result = runCatching {
                moveNodesToRubbishUseCase(nodeHandles)
            }.onFailure {
                Timber.e(it)
            }
            _state.update { state -> state.copy(moveRequestResult = result) }
        }
    }

    /**
     * Mark handle move request result
     *
     */
    fun markHandleMoveRequestResult() {
        _state.update { it.copy(moveRequestResult = null) }
    }

    /**
     * Copy or Move nodes
     */
    fun copyOrMoveNodes(nodes: List<Long>, targetNode: Long, type: NodeNameCollisionType) {
        if (!isOnline()) {
            _state.update { it.copy(snackBarMessage = R.string.error_server_connection_problem) }
            return
        }
        val alertText = when (type) {
            NodeNameCollisionType.MOVE -> R.string.context_moving
            NodeNameCollisionType.COPY -> R.string.context_copying
            else -> null
        }
        _state.update { it.copy(copyMoveAlertTextId = alertText) }
        val nodeMap = nodes.associateWith { targetNode }
        checkNameCollision(nodeMap, type)
    }

    private fun checkNameCollision(nodeMap: Map<Long, Long>, type: NodeNameCollisionType) =
        viewModelScope.launch {
            runCatching {
                checkNodesNameCollisionUseCase(nodes = nodeMap, type = type)
            }.onSuccess { result ->
                updateStateWithConflictNodes(result)
                initiateCopyOrMoveForNonConflictNodes(result)
            }.onFailure {
                Timber.e(it)
                _state.update { state -> state.copy(copyMoveAlertTextId = null) }
            }
        }

    private suspend fun initiateCopyOrMoveForNonConflictNodes(result: NodeNameCollisionResult) {
        if (result.type == NodeNameCollisionType.MOVE) {
            moveNodes(result.noConflictNodes)
        } else {
            copyNodes(result.noConflictNodes)
        }
    }

    private fun updateStateWithConflictNodes(result: NodeNameCollisionResult) = runCatching {
        result.conflictNodes.values.map {
            when (result.type) {
                NodeNameCollisionType.MOVE -> NameCollision.Movement.getMovementCollision(it)
                NodeNameCollisionType.COPY -> NameCollision.Copy.getCopyCollision(it)
                else -> throw UnsupportedOperationException("Invalid collision result")
            }
        }
    }.onSuccess { collisions ->
        _state.update {
            it.copy(copyMoveAlertTextId = null, nodeNameCollisionResult = collisions)
        }
    }.onFailure {
        Timber.e(it)
    }

    /**
     * Mark handle node name collision result
     *
     */
    fun markHandleNodeNameCollisionResult() {
        _state.update { it.copy(nodeNameCollisionResult = emptyList()) }
    }

    /**
     * Move nodes
     *
     * @param nodes
     */
    private suspend fun moveNodes(nodes: Map<Long, Long>) {
        val result = runCatching {
            moveNodesUseCase(nodes)
        }.onFailure { Timber.e(it) }
        _state.update { state -> state.copy(moveRequestResult = result) }
    }


    private suspend fun copyNodes(nodes: Map<Long, Long>) {
        val result = runCatching {
            copyNodesUseCase(nodes)
        }.onFailure { Timber.e(it) }
        _state.update {
            it.copy(moveRequestResult = result, copyMoveAlertTextId = null)
        }
    }

    /**
     * on Consume Snack Bar Message event
     */
    fun onConsumeSnackBarMessageEvent() {
        viewModelScope.launch {
            _state.update { it.copy(snackBarMessage = null) }
        }
    }
}
