package mega.privacy.android.app.presentation.node.dialogs.deletenode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.movenode.mapper.MoveRequestMessageMapper
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import mega.privacy.android.domain.usecase.node.DeleteNodesUseCase
import mega.privacy.android.domain.usecase.node.MoveNodesToRubbishUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * View Model for Move to rubbish or delete view Model
 * @property deleteNodesUseCase [DeleteNodesUseCase]
 * @property moveNodesToRubbishUseCase [MoveNodesToRubbishUseCase]
 * @property moveRequestMessageMapper [MoveRequestMessageMapper]
 */
@HiltViewModel
class MoveToRubbishOrDeleteNodeDialogViewModel @Inject constructor(
    private val deleteNodesUseCase: DeleteNodesUseCase,
    private val moveNodesToRubbishUseCase: MoveNodesToRubbishUseCase,
    private val moveRequestMessageMapper: MoveRequestMessageMapper,
) : ViewModel() {

    private val _state = MutableStateFlow(MoveToRubbishOrDeleteNodesState())

    /**
     * state for MoveToRubbishOrDeleteNodeDialog
     */
    val state: StateFlow<MoveToRubbishOrDeleteNodesState> = _state.asStateFlow()

    /**
     * Delete nodes
     *
     * @param nodeHandles
     */
    fun deleteNodes(handles: List<Long>) {
        viewModelScope.launch {
            val nodeHandles = handles.map {
                NodeId(it)
            }
            val result = runCatching {
                deleteNodesUseCase(nodeHandles)
            }.onFailure {
                Timber.e(it)
            }
            result.getOrNull()?.let { res ->
                val message = moveRequestMessageMapper(res)
                _state.update {
                    it.copy(
                        deleteEvent = triggered(message)
                    )
                }
            }
        }
    }

    /**
     * Move nodes to rubbish
     *
     * @param nodeHandles
     */
    fun moveNodesToRubbishBin(handles: List<Long>) {
        viewModelScope.launch {
            val handlesList = handles.map {
                it
            }
            val result = runCatching {
                moveNodesToRubbishUseCase(handlesList)
            }.onFailure {
                Timber.e(it)
            }
            result.getOrNull()?.let { res ->
                val message = moveRequestMessageMapper(res)
                _state.update {
                    it.copy(
                        deleteEvent = triggered(message)
                    )
                }
            }
        }
    }

    /**
     * Consumed Delete event
     */
    fun consumeDeleteEvent() {
        _state.update {
            it.copy(deleteEvent = consumed())
        }
    }
}