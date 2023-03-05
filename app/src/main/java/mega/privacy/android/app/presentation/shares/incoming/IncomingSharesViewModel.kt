package mega.privacy.android.app.presentation.shares.incoming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.AuthorizeNode
import mega.privacy.android.app.domain.usecase.GetIncomingSharesChildrenNode
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.presentation.shares.incoming.model.IncomingSharesState
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetOthersSortOrder
import mega.privacy.android.domain.usecase.GetParentNodeHandle
import mega.privacy.android.domain.usecase.GetUnverifiedIncomingShares
import mega.privacy.android.domain.usecase.MonitorContactUpdates
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.util.Stack
import javax.inject.Inject

/**
 * ViewModel associated to IncomingSharesFragment
 *
 * @param monitorContactUpdates monitor contact update when credentials verification occurs to update shares list
 */
@HiltViewModel
class IncomingSharesViewModel @Inject constructor(
    private val getNodeByHandle: GetNodeByHandle,
    private val authorizeNode: AuthorizeNode,
    private val getParentNodeHandle: GetParentNodeHandle,
    private val getIncomingSharesChildrenNode: GetIncomingSharesChildrenNode,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val getOthersSortOrder: GetOthersSortOrder,
    monitorNodeUpdates: MonitorNodeUpdates,
    monitorContactUpdates: MonitorContactUpdates,
    private val getUnverifiedIncomingShares: GetUnverifiedIncomingShares,
) : ViewModel() {

    /** private UI state */
    private val _state = MutableStateFlow(IncomingSharesState())

    /** public UI state */
    val state: StateFlow<IncomingSharesState> = _state

    /**
     * Serves as the original View Type.
     * When an update from MonitorViewType is received, this value is used to determine if the View Type changed & also updated
     */
    var isList = true
        private set

    /** stack of scroll position for each depth */
    private val lastPositionStack: Stack<Int> = Stack<Int>()

    init {
        refreshIncomingSharesNode()

        viewModelScope.launch {
            monitorNodeUpdates().collect { list ->
                Timber.d("Received node update")
                // If the current incoming handle is the node that was updated,
                // check if the current user still has access to it,
                // if not redirect to root incoming shares
                list.changes.keys
                    .filter { it.isIncomingShare }
                    .singleOrNull { it.id.longValue == _state.value.incomingHandle }
                    ?.let { node ->
                        (getNodeByHandle(node.id.longValue) ?: authorizeNode(node.id.longValue))
                            .takeIf { it == null }
                            .let {
                                resetIncomingTreeDepth()
                            }
                    }
                refreshIncomingSharesNode()
            }
        }
        viewModelScope.launch {
            monitorContactUpdates().collectLatest { updates ->
                Timber.d("Received contact update")
                if (updates.changes.values.any { it.contains(UserChanges.AuthenticationInformation) }) {
                    refreshIncomingSharesNode()
                }
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
     * Set isList when update from MonitorViewType is received
     */
    fun setIsList(value: Boolean) {
        isList = value
    }

    /**
     * Decrease by 1 the incoming tree depth
     *
     * @param handle the id of the current incoming handle to set
     */
    fun decreaseIncomingTreeDepth(handle: Long) = viewModelScope.launch {
        setIncomingTreeDepth(_state.value.incomingTreeDepth - 1, handle)
    }

    /**
     * Increase by 1 the incoming tree depth
     *
     * @param handle the id of the current incoming handle to set
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
     * @param handle the id of the current incoming handle to set
     */
    fun setIncomingTreeDepth(depth: Int, handle: Long) = viewModelScope.launch {
        _state.update {
            it.copy(
                isLoading = true,
                incomingTreeDepth = depth,
                incomingHandle = handle,
                incomingParentHandle = getParentNodeHandle(handle),
                sortOrder = if (depth == 0) getOthersSortOrder() else
                    getCloudSortOrder()
            )
        }

        // refresh nodes and update state with updated nodes
        _state.update {
            refreshNodes(handle)?.let { nodes ->
                it.copy(
                    nodes = nodes,
                    isInvalidHandle = isInvalidHandle(handle),
                    isLoading = false
                )
            } ?: run {
                it.copy(
                    nodes = emptyList(),
                    incomingTreeDepth = 0,
                    incomingHandle = -1L,
                    isInvalidHandle = true,
                    isLoading = false,
                    incomingParentHandle = null,
                    sortOrder = getOthersSortOrder()
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
    private suspend fun refreshNodes(handle: Long = _state.value.incomingHandle): List<MegaNode>? {
        Timber.d("refreshIncomingSharesNodes")
        val unverifiedIncomingShares = getUnverifiedIncomingShares(_state.value.sortOrder)
            .filter { shareData -> !isInvalidHandle(shareData.nodeHandle) }
        val handles = unverifiedIncomingShares.map { shareData -> shareData.nodeHandle }
        _state.update {
            it.copy(
                unverifiedIncomingShares = unverifiedIncomingShares,
                unVerifiedIncomingNodeHandles = handles
            )
        }

        val unverifiedIncomingSharesNodes = handles.mapNotNull { getNodeByHandle(it) }
        val incomingSharesNodes = getIncomingSharesChildrenNode(handle)
        return if (
            unverifiedIncomingSharesNodes.isNotEmpty() || incomingSharesNodes.isNullOrEmpty().not()
        )
            mutableListOf<MegaNode>().apply {
                addAll(unverifiedIncomingSharesNodes)
                incomingSharesNodes?.let { addAll(it) }
            }.distinctBy { it.handle } else null
    }

    /**
     * Check if the handle is valid or not
     *
     * @param handle
     * @return true if the handle is invalid
     */
    private suspend fun isInvalidHandle(handle: Long = _state.value.incomingHandle): Boolean {
        return handle
            .takeUnless { it == -1L || it == INVALID_HANDLE }
            ?.let { getNodeByHandle(it) == null }
            ?: true
    }
}
