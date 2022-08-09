package mega.privacy.android.app.presentation.shares.incoming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.domain.usecase.AuthorizeNode
import mega.privacy.android.app.domain.usecase.GetIncomingSharesChildrenNode
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.presentation.shares.incoming.model.IncomingSharesState
import mega.privacy.android.domain.usecase.GetParentNodeHandle
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.util.Stack
import javax.inject.Inject

/**
 * ViewModel associated to IncomingSharesFragment
 */
@HiltViewModel
class IncomingSharesViewModel @Inject constructor(
    private val getNodeByHandle: GetNodeByHandle,
    private val authorizeNode: AuthorizeNode,
    private val getParentNodeHandle: GetParentNodeHandle,
    private val getIncomingSharesChildrenNode: GetIncomingSharesChildrenNode,
    monitorNodeUpdates: MonitorNodeUpdates,
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
            monitorNodeUpdates().collect { list ->
                // If the current incoming parent handle is the node that was updated,
                // check if the current user still has access to it,
                // if not redirect to root incoming shares
                list
                    .filter { it.isInShare }
                    .singleOrNull { it.handle == _state.value.incomingParentHandle }
                    ?.let { node ->
                        (getNodeByHandle(node.handle) ?: authorizeNode(node.handle))
                            .takeIf { it == null }
                            .let {
                                resetIncomingTreeDepth()
                            }
                    }

                // Uncomment this line once LinksFragment is decoupled from ManagerActivity
                //refreshNodes()?.let { setNodes(it) }
            }
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
     *
     * @param handle the id of the current outgoing parent handle to set
     */
    fun decreaseIncomingTreeDepth(handle: Long) = viewModelScope.launch {
        setIncomingTreeDepth(_state.value.incomingTreeDepth - 1, handle)
    }

    /**
     * Increase by 1 the incoming tree depth
     *
     * @param handle the id of the current outgoing parent handle to set
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
     * If refresh nodes return null, else display empty list
     *
     * @param depth the tree depth value to set
     * @param handle the id of the current outgoing parent handle to set
     */
    fun setIncomingTreeDepth(depth: Int, handle: Long) = viewModelScope.launch {
        _state.update {
            it.copy(
                isLoading = true,
                incomingTreeDepth = depth,
                incomingParentHandle = handle
            )
        }

        // refresh nodes and update state with updated nodes
        _state.update {
            refreshNodes(handle)?.let { nodes ->
                it.copy(
                    nodes = nodes,
                    isInvalidParentHandle = isInvalidParentHandle(handle),
                    isLoading = false
                )
            } ?: run {
                it.copy(
                    nodes = emptyList(),
                    incomingTreeDepth = 0,
                    incomingParentHandle = -1L,
                    isInvalidParentHandle = true,
                    isLoading = false
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
    fun pushToLastPositionStack(position: Int): Int = lastPositionStack.push(position)

    /**
     * Get the parent node handle of current node
     *
     * @return the parent node handle of current node
     */
    fun getParentNodeHandle(): Long? = runBlocking {
        return@runBlocking getParentNodeHandle(_state.value.incomingParentHandle)
    }

    /**
     * Set the current nodes displayed
     *
     * @param nodes the list of nodes to set
     */
    private fun setNodes(nodes: List<MegaNode>) {
        _state.update { it.copy(nodes = nodes, isLoading = false) }
    }

    /**
     * Refresh the list of nodes from api
     *
     * @param handle
     */
    private suspend fun refreshNodes(handle: Long = _state.value.incomingParentHandle): List<MegaNode>? {
        Timber.d("refreshIncomingSharesNodes")
        return getIncomingSharesChildrenNode(handle)
    }

    /**
     * Check if the parent handle is valid
     *
     * @param handle
     * @return true if the parent handle is valid
     */
    private suspend fun isInvalidParentHandle(handle: Long = _state.value.incomingParentHandle): Boolean {
        return handle
            .takeUnless { it == -1L || it == INVALID_HANDLE }
            ?.let { getNodeByHandle(it) == null }
            ?: true
    }

}