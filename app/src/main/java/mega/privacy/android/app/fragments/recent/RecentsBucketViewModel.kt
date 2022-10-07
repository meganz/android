package mega.privacy.android.app.fragments.recent

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.app.domain.usecase.GetParentMegaNode
import mega.privacy.android.app.domain.usecase.GetRecentActionNodes
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.domain.usecase.UpdateRecentAction
import mega.privacy.android.app.fragments.homepage.NodeItem
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRecentActionBucket
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel associated to [RecentsBucketFragment]
 */
@HiltViewModel
class RecentsBucketViewModel @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val getParentMegaNode: GetParentMegaNode,
    private val updateRecentAction: UpdateRecentAction,
    private val getRecentActionNodes: GetRecentActionNodes,
    monitorNodeUpdates: MonitorNodeUpdates,
) : ViewModel() {
    private val _actionMode = MutableLiveData<Boolean>()

    /**
     * True if the actionMode should to be visible
     */
    val actionMode: LiveData<Boolean> = _actionMode

    private val _nodesToAnimate = MutableLiveData<Set<Int>>()

    /**
     * Set of node positions to animate
     */
    val nodesToAnimate: LiveData<Set<Int>> = _nodesToAnimate

    private val selectedNodes: MutableSet<NodeItem> = mutableSetOf()

    /**
     * Current bucket
     */
    private val _bucket: MutableStateFlow<MegaRecentActionBucket?> = MutableStateFlow(null)
    val bucket = _bucket.asStateFlow()

    private var cachedActionList: List<MegaRecentActionBucket>? = null

    private val _shouldCloseFragment: MutableLiveData<Boolean> = MutableLiveData(false)

    /**
     * True if the fragment needs to be closed
     */
    val shouldCloseFragment: LiveData<Boolean> = _shouldCloseFragment

    /**
     * True if the parent of the bucket is an incoming shares
     */
    var isInShare = false

    /**
     *  List of node items in the current bucket
     */
    val items = _bucket
        .map { it?.let { getRecentActionNodes(it.nodes) } ?: emptyList() }
        .onEach {
            isInShare = it.firstOrNull()?.node?.let { node ->
                getParentMegaNode(node)?.isInShare
            } ?: false
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            monitorNodeUpdates().collectLatest {
                Timber.d("Received node update")
                updateCurrentBucket()
                clearSelection()
            }
        }
    }

    /**
     * Set bucket value
     *
     * @param selectedBucket
     */
    fun setBucket(selectedBucket: MegaRecentActionBucket?) = viewModelScope.launch {
        _bucket.emit(selectedBucket)
    }

    /**
     * Set cached action list
     *
     * @param cachedActions
     */
    fun setCachedActionList(cachedActions: List<MegaRecentActionBucket>?) {
        cachedActionList = cachedActions
    }

    /**
     * Get the selected nodes
     *
     * @return the selected nodes
     */
    fun getSelectedNodes(): List<NodeItem> = selectedNodes.toList()

    /**
     * Retrieves the list of non-null [MegaNode] objects from [selectedNodes]
     *
     * @return A list of nun-null [MegaNode] objects
     */
    fun getSelectedMegaNodes(): List<MegaNode> =
        selectedNodes.toList().mapNotNull { it.node }

    /**
     * Checks whether any [MegaNode] in [getSelectedMegaNodes] belongs in Backups
     *
     * @return True if at least one [MegaNode] belongs in Backups, and False if otherwise
     */
    fun isAnyNodeInBackups(): Boolean =
        getSelectedMegaNodes().any { node -> megaApi.isInInbox(node) }

    /**
     * Get the count of selected nodes
     *
     * @return the count of selected nodes
     */
    fun getSelectedNodesCount(): Int = selectedNodes.size

    /**
     * Get the count of nodes
     *
     * @return the count of nodes
     */
    fun getNodesCount(): Int = items.value.size

    /**
     * Clear selected nodes
     */
    fun clearSelection() {
        _actionMode.value = false
        selectedNodes.clear()

        val animNodeIndices = mutableSetOf<Int>()
        val nodeList = items.value

        for ((position, node) in nodeList.withIndex()) {
            if (node in selectedNodes) {
                animNodeIndices.add(position)
            }
            node.selected = false
            node.uiDirty = true
        }

        _nodesToAnimate.value = animNodeIndices
    }

    /**
     * Receive on node long click
     *
     * @param position the position of the item in the adapter
     * @param node the node item
     */
    fun onNodeLongClicked(position: Int, node: NodeItem) {
        val nodeList = items.value

        if (position < 0 || position >= nodeList.size || nodeList[position].hashCode() != node.hashCode()
        ) {
            return
        }

        nodeList[position].selected = !nodeList[position].selected

        if (nodeList[position] !in selectedNodes) {
            selectedNodes.add(node)
        } else {
            selectedNodes.remove(node)
        }

        nodeList[position].uiDirty = true
        _actionMode.value = selectedNodes.isNotEmpty()

        _nodesToAnimate.value = hashSetOf(position)
    }

    /**
     * Select all nodes
     */
    fun selectAll() {
        val nodeList = items.value

        val animNodeIndices = mutableSetOf<Int>()

        for ((position, node) in nodeList.withIndex()) {
            if (!node.selected) {
                animNodeIndices.add(position)
            }
            node.selected = true
            node.uiDirty = true
            selectedNodes.add(node)
        }

        _nodesToAnimate.value = animNodeIndices
        _actionMode.value = true
    }

    /**
     * Update the current bucket
     */
    private suspend fun updateCurrentBucket() {
        _bucket.value
            ?.let { updateRecentAction(it, cachedActionList) }
            ?.let { _bucket.emit(it) }
            ?: run {
                // No nodes contained in the bucket or the action bucket is no loner exists.
                _shouldCloseFragment.postValue(true)
            }
    }
}

