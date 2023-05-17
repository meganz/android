package mega.privacy.android.app.presentation.shares.outgoing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.domain.usecase.GetOutgoingSharesChildrenNode
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.presentation.shares.incoming.model.IncomingSharesState
import mega.privacy.android.app.presentation.shares.outgoing.model.OutgoingSharesState
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetOthersSortOrder
import mega.privacy.android.domain.usecase.GetParentNodeHandle
import mega.privacy.android.domain.usecase.MonitorContactUpdates
import mega.privacy.android.domain.usecase.shares.GetUnverifiedOutgoingShares
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.util.Stack
import javax.inject.Inject

/**
 * ViewModel associated to OutgoingSharesFragment
 *
 * @param monitorContactUpdates monitor contact update when credentials verification occurs to update shares list
 */
@HiltViewModel
class OutgoingSharesViewModel @Inject constructor(
    private val getNodeByHandle: GetNodeByHandle,
    private val getParentNodeHandle: GetParentNodeHandle,
    private val getOutgoingSharesChildrenNode: GetOutgoingSharesChildrenNode,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val getOthersSortOrder: GetOthersSortOrder,
    monitorNodeUpdates: MonitorNodeUpdates,
    monitorContactUpdates: MonitorContactUpdates,
    private val getUnverifiedOutgoingShares: GetUnverifiedOutgoingShares,
) : ViewModel() {

    /** private UI state */
    private val _state = MutableStateFlow(OutgoingSharesState())

    /** public UI state */
    val state: StateFlow<OutgoingSharesState> = _state

    /** stack of scroll position for each depth */
    private val lastPositionStack: Stack<Int> = Stack<Int>()

    init {
        refreshOutgoingSharesNode()

        viewModelScope.launch {
            monitorNodeUpdates().collectLatest {
                Timber.d("Received node update")
                refreshOutgoingSharesNode()
            }
        }
        viewModelScope.launch {
            monitorContactUpdates().collectLatest { updates ->
                Timber.d("Received contact update")
                if (updates.changes.values.any { it.contains(UserChanges.AuthenticationInformation) }) {
                    refreshOutgoingSharesNode()
                }
            }
        }
    }

    /**
     * Refresh outgoing shares node
     */
    fun refreshOutgoingSharesNode() = viewModelScope.launch {
        refreshNodes()?.let { setNodes(it) }
    }

    /**
     * Updates the value of [IncomingSharesState.currentViewType]
     *
     * @param newViewType The new [ViewType]
     */
    fun setCurrentViewType(newViewType: ViewType) {
        _state.update { it.copy(currentViewType = newViewType) }
    }

    /**
     * Decrease by 1 the outgoing tree depth
     *
     * @param handle the id of the current outgoing handle to set
     */
    fun decreaseOutgoingTreeDepth(handle: Long) = viewModelScope.launch {
        setOutgoingTreeDepth(_state.value.outgoingTreeDepth - 1, handle)
    }

    /**
     * Increase by 1 the outgoing tree depth
     *
     * @param handle the id of the current outgoing handle to set
     */
    fun increaseOutgoingTreeDepth(handle: Long) = viewModelScope.launch {
        setOutgoingTreeDepth(_state.value.outgoingTreeDepth + 1, handle)
    }

    /**
     * Reset outgoing tree depth to initial value
     */
    fun resetOutgoingTreeDepth() = viewModelScope.launch {
        setOutgoingTreeDepth(0, -1L)
    }

    /**
     * Set outgoing tree depth with given value
     * If refresh nodes return null, else display empty list
     *
     * @param depth the tree depth value to set
     * @param handle the id of the current outgoing handle to set
     */
    private fun setOutgoingTreeDepth(depth: Int, handle: Long) = viewModelScope.launch {
        _state.update {
            it.copy(
                isLoading = true,
                outgoingTreeDepth = depth,
                outgoingHandle = handle,
                outgoingParentHandle = getParentNodeHandle(handle),
                sortOrder = if (depth == 0) getOthersSortOrder() else
                    getCloudSortOrder()
            )
        }

        _state.update {
            refreshNodes(handle)?.let { nodes ->
                it.copy(
                    nodes = nodes,
                    outgoingTreeDepth = depth,
                    outgoingHandle = handle,
                    isInvalidHandle = isInvalidHandle(handle),
                    isLoading = false
                )
            } ?: run {
                it.copy(
                    nodes = emptyList(),
                    outgoingTreeDepth = 0,
                    outgoingHandle = -1L,
                    isInvalidHandle = true,
                    isLoading = false,
                    outgoingParentHandle = null,
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
    private fun setNodes(nodes: List<Pair<MegaNode, ShareData?>>) {
        _state.update { it.copy(nodes = nodes, isLoading = false) }
    }

    /**
     * Refresh the list of nodes from api
     *
     * @param handle
     * @return a list of Pair<MegaNode, ShareData?>
     *         ShareData is null if the MegaNode is already verified
     *         A node that is shared among multiple users and not verified by them will produce
     *         distinct elements
     */
    private suspend fun refreshNodes(handle: Long = _state.value.outgoingHandle): List<Pair<MegaNode, ShareData?>>? {
        Timber.d("refreshOutgoingSharesNodes")

        val unverifiedNodes = getOutgoingUnverifiedNodes()
        val verifiedNodes = getOutgoingVerifiedNodes(handle)

        return when {
            unverifiedNodes?.isNotEmpty() == true && verifiedNodes?.isNotEmpty() == true -> unverifiedNodes + verifiedNodes
            unverifiedNodes?.isNotEmpty() == true -> unverifiedNodes
            else -> verifiedNodes
        }
    }

    /**
     *  Get the list of unverified outgoing nodes if the tree depth is 0, else return null
     *
     *  If one specific node is shared among multiple users,
     *  this function will return a list with distinct element for each user who was not verified for
     *  this particular node, defined by the unique pair Node and ShareData
     *
     *  @return a list of Pair of MegaNode, ShareData
     */
    private suspend fun getOutgoingUnverifiedNodes(): List<Pair<MegaNode, ShareData>>? =
        if (state.value.outgoingTreeDepth == 0) {
            getUnverifiedOutgoingShares(_state.value.sortOrder)
                .filter { shareData -> !isInvalidHandle(shareData.nodeHandle) }
                .mapNotNull { shareData ->
                    getNodeByHandle(shareData.nodeHandle)?.let {
                        Pair(it, shareData)
                    }
                }
        } else {
            null
        }

    /**
     *  Get the list of outgoing nodes
     *
     *  ShareData will be null in order to display them as verified nodes
     *
     *  @return a list of Pair of MegaNode, ShareData
     */
    private suspend fun getOutgoingVerifiedNodes(handle: Long): List<Pair<MegaNode, ShareData?>>? =
        getOutgoingSharesChildrenNode(handle)?.map { Pair<MegaNode, ShareData?>(it, null) }

    /**
     * Check if the handle is valid or not
     *
     * @param handle
     * @return true if the handle is invalid
     */
    private suspend fun isInvalidHandle(handle: Long = _state.value.outgoingHandle): Boolean {
        return handle
            .takeUnless { it == MegaApiJava.INVALID_HANDLE }
            ?.let { getNodeByHandle(it) == null }
            ?: true
    }
}
