package mega.privacy.android.app.presentation.shares.incoming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.GetIncomingSharesChildrenNode
import mega.privacy.android.app.presentation.shares.incoming.model.IncomingSharesState
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.util.Stack
import javax.inject.Inject

/**
 * ViewModel associated to IncomingSharesFragment
 */
@HiltViewModel
class IncomingSharesViewModel @Inject constructor(
//    private val getNodeByHandle: GetNodeByHandle,
//    private val authorizeNode: AuthorizeNode,
    private val getIncomingSharesChildrenNode: GetIncomingSharesChildrenNode,
//    monitorNodeUpdates: MonitorNodeUpdates,
) : ViewModel() {

    /** private UI state */
    private val _state = MutableStateFlow(IncomingSharesState())

    /** public UI state */
    val state: StateFlow<IncomingSharesState> = _state

    /** stack of scroll position for each depth */
    private val lastPositionStack: Stack<Int> = Stack<Int>()

    init {
        viewModelScope.launch {
            refreshNodes()?.let { setNodes(it) }
//            monitorNodeUpdates().collect { list ->
//                list
//                    .filter { it.isInShare }
//                    .singleOrNull { it.handle == _state.value.incomingParentHandle }
//                    ?.let { node ->
//                        getNodeByHandle(node.handle) ?: authorizeNode(node.handle)
//                            .takeIf { it == null }
//                            .let {
//                                resetIncomingTreeDepth(MegaApiJava.INVALID_HANDLE)
//                            }
//                    }
//                refreshNodes()
//            }
        }
    }


    /**
     * Refresh incoming shares node
     */
    fun refreshIncomingSharesNode() = viewModelScope.launch {
        refreshNodes()?.let { setNodes(it) }
    }

    /**
     * Decrease by 1 the incoming tree depth
     */
    fun decreaseIncomingTreeDepth(handle: Long) = viewModelScope.launch {
        setIncomingTreeDepth(_state.value.incomingTreeDepth - 1, handle)
    }

    /**
     * Increase by 1 the incoming tree depth
     */
    fun increaseIncomingTreeDepth(handle: Long) = viewModelScope.launch {
        setIncomingTreeDepth(_state.value.incomingTreeDepth + 1, handle)

    }

    /**
     * Reset incoming tree depth to initial value
     */
    fun resetIncomingTreeDepth() = viewModelScope.launch {
        setIncomingTreeDepth(0, -1L)
    }

    /**
     * Set incoming tree depth with given value
     *
     * @param depth the tree depth value to set
     */
    fun setIncomingTreeDepth(depth: Int, handle: Long) = viewModelScope.launch {
        _state.update {
            refreshNodes(handle)?.let { nodes ->
                it.copy(nodes = nodes, incomingTreeDepth = depth, incomingParentHandle = handle)
            } ?: run {
                it.copy(
                    nodes = emptyList(),
                    incomingTreeDepth = 0,
                    incomingParentHandle = -1L
                )
            }
        }
    }

    /**
     * Pop scroll position for previous depth
     *
     * @return last position saved
     */
    fun popLastPositionStack(): Int = lastPositionStack.takeIf { it.isNotEmpty() }?.pop() ?: 0

    /**
     * Push scroll position for current depth
     *
     * @param position the scroll position of the recyclerView for the current depth
     * @return the position saved
     */
    fun pushToLastPositionState(position: Int): Int = lastPositionStack.push(position)

    /**
     * Set the current nodes displayed
     *
     * @param nodes the list of nodes to set
     */
    private fun setNodes(nodes: List<MegaNode>) {
        _state.update { it.copy(nodes = nodes) }
    }

    /**
     * Refresh the list of nodes from api
     */
    private suspend fun refreshNodes(handle: Long = _state.value.incomingParentHandle): List<MegaNode>? {
        Timber.d("refreshIncomingSharesNodes")
        return getIncomingSharesChildrenNode(handle)
    }
}