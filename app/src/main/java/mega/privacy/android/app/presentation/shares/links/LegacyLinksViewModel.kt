package mega.privacy.android.app.presentation.shares.links

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.domain.usecase.GetPublicLinks
import mega.privacy.android.app.presentation.shares.links.model.LegacyLinksState
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetLinksSortOrder
import mega.privacy.android.domain.usecase.GetParentNodeUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.util.Stack
import javax.inject.Inject

/**
 * ViewModel associated to LinksFragment
 */
@HiltViewModel
class LegacyLinksViewModel @Inject constructor(
    private val getNodeByHandle: GetNodeByHandle,
    private val getParentNodeUseCase: GetParentNodeUseCase,
    private val getPublicLinks: GetPublicLinks,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val getLinksSortOrder: GetLinksSortOrder,
    monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
) : ViewModel() {

    /** private UI state */
    private val _state = MutableStateFlow(LegacyLinksState())

    /** public UI state */
    val state: StateFlow<LegacyLinksState> = _state

    /** stack of scroll position for each depth */
    private val lastPositionStack: Stack<Int> = Stack<Int>()

    init {
        viewModelScope.launch {
            refreshNodes()?.let { setNodes(it) }
            monitorNodeUpdatesUseCase().collect {
                Timber.d("Received node update")
                refreshNodes()?.let { setNodes(it) }
            }
        }
    }

    /**
     * A shorthand way of retrieving the [LegacyLinksState]
     *
     * @return the [LegacyLinksState]
     */
    fun state() = _state.value

    /**
     * Refresh links shares node
     */
    fun refreshLinksSharesNode() = viewModelScope.launch {
        refreshNodes()?.let { setNodes(it) }
    }

    /**
     * Decrease by 1 the links tree depth
     *
     * @param handle the id of the current links handle to set
     */
    fun decreaseLinksTreeDepth(handle: Long) = viewModelScope.launch {
        setLinksTreeDepth(_state.value.linksTreeDepth - 1, handle)
    }

    /**
     * Increase by 1 the links tree depth
     *
     * @param handle the id of the current links handle to set
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
     * @param handle the id of the current links handle to set
     */
    private suspend fun setLinksTreeDepth(depth: Int, handle: Long) {
        _state.update {
            it.copy(
                isLoading = true,
                linksTreeDepth = depth,
                linksHandle = handle,
                linksParentHandle = getParentNodeUseCase(NodeId(handle))?.id?.longValue,
                sortOrder = if (depth == 0) getLinksSortOrder() else
                    getCloudSortOrder()
            )
        }

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
                    linksTreeDepth = 0,
                    linksHandle = -1L,
                    isInvalidHandle = true,
                    isLoading = false,
                    linksParentHandle = null,
                    sortOrder = getLinksSortOrder()
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
    private suspend fun refreshNodes(handle: Long = _state.value.linksHandle): List<MegaNode>? {
        Timber.d("refreshPublicLinks")
        return getPublicLinks(handle)
    }

    /**
     * Check if the handle is valid or not
     *
     * @param handle
     * @return true if the handle is invalid
     */
    private suspend fun isInvalidHandle(handle: Long = _state.value.linksHandle): Boolean {
        return handle
            .takeUnless { it == -1L || it == MegaApiJava.INVALID_HANDLE }
            ?.let { getNodeByHandle(it) == null }
            ?: true
    }
}
