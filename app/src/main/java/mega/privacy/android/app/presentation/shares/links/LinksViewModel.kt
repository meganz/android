package mega.privacy.android.app.presentation.shares.links

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.domain.usecase.GetPublicLinks
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.presentation.shares.links.model.LinksState
import mega.privacy.android.domain.usecase.GetParentNodeHandle
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.util.Stack
import javax.inject.Inject

/**
 * ViewModel associated to LinksFragment
 */
@HiltViewModel
class LinksViewModel @Inject constructor(
    private val getNodeByHandle: GetNodeByHandle,
    private val getParentNodeHandle: GetParentNodeHandle,
    private val getPublicLinks: GetPublicLinks,
    monitorNodeUpdates: MonitorNodeUpdates,
) : ViewModel() {

    /** private UI state */
    private val _state = MutableStateFlow(LinksState())

    /** public UI state */
    val state: StateFlow<LinksState> = _state

    /** stack of scroll position for each depth */
    private val lastPositionStack: Stack<Int> = Stack<Int>()

    init {
        viewModelScope.launch {
            refreshNodes()?.let { setNodes(it) }
            monitorNodeUpdates().collect {
                // Uncomment this line once LinksFragment is decoupled from ManagerActivity
                //refreshNodes()?.let { setNodes(it) }
            }
        }
    }

    /**
     * Refresh links shares node
     */
    fun refreshLinksSharesNode() = viewModelScope.launch {
        refreshNodes()?.let { setNodes(it) }
    }

    /**
     * Decrease by 1 the links tree depth
     *
     * @param handle the id of the current outgoing parent handle to set
     */
    fun decreaseLinksTreeDepth(handle: Long) = viewModelScope.launch {
        setLinksTreeDepth(_state.value.linksTreeDepth - 1, handle)
    }

    /**
     * Increase by 1 the links tree depth
     *
     * @param handle the id of the current outgoing parent handle to set
     */
    fun increaseLinksTreeDepth(handle: Long) = viewModelScope.launch {
        setLinksTreeDepth(_state.value.linksTreeDepth + 1, handle)
    }

    /**
     * Reset links tree depth to initial value
     */
    fun resetLinksTreeDepth() = viewModelScope.launch {
        setLinksTreeDepth(0, -1L)
    }

    /**
     * Set links tree depth with given value
     * If refresh nodes return null, else display empty list
     *
     * @param depth the tree depth value to set
     * @param handle the id of the current outgoing parent handle to set
     */
    private suspend fun setLinksTreeDepth(depth: Int, handle: Long) {
        _state.update {
            it.copy(
                isLoading = true,
                linksTreeDepth = depth,
                linksParentHandle = handle,
            )
        }

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
                    linksTreeDepth = 0,
                    linksParentHandle = -1L,
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
    fun pushToLastPositionState(position: Int): Int = lastPositionStack.push(position)

    /**
     * Get the parent node handle of current node
     *
     * @return the parent node handle of current node
     */
    fun getParentNodeHandle(): Long? = runBlocking {
        return@runBlocking getParentNodeHandle(_state.value.linksParentHandle)
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
    private suspend fun refreshNodes(handle: Long = _state.value.linksParentHandle): List<MegaNode>? {
        Timber.d("refreshOutgoingSharesNodes")
        return getPublicLinks(handle)
    }

    /**
     * Check if the parent handle is valid
     *
     * @param handle
     * @return true if the parent handle is valid
     */
    private suspend fun isInvalidParentHandle(handle: Long = _state.value.linksParentHandle): Boolean {
        return handle
            .takeUnless { it == -1L || it == MegaApiJava.INVALID_HANDLE }
            ?.let { getNodeByHandle(it) == null }
            ?: true
    }

}