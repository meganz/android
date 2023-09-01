package mega.privacy.android.app.presentation.contact

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.node.CheckNodesNameCollisionUseCase
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
     * Check move nodes name collision
     *
     * @param nodes
     * @param targetNode
     */
    fun checkMoveNodesNameCollision(nodes: List<Long>, targetNode: Long) {
        viewModelScope.launch {
            runCatching {
                checkNodesNameCollisionUseCase(
                    nodes.associateWith { targetNode },
                    NodeNameCollisionType.MOVE
                )
            }.onSuccess { result ->
                _state.update { it.copy(nodeNameCollisionResult = result) }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Mark handle node name collision result
     *
     */
    fun markHandleNodeNameCollisionResult() {
        _state.update { it.copy(nodeNameCollisionResult = null) }
    }

    /**
     * Move nodes
     *
     * @param nodes
     */
    fun moveNodes(nodes: Map<Long, Long>) {
        viewModelScope.launch {
            val result = runCatching {
                moveNodesUseCase(nodes)
            }.onFailure {
                Timber.e(it)
            }
            _state.update { state -> state.copy(moveRequestResult = result) }
        }
    }
}
