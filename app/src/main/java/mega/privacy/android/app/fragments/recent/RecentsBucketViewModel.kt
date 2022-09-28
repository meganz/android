package mega.privacy.android.app.fragments.recent

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.fragments.homepage.NodeItem
import mega.privacy.android.app.utils.MegaNodeUtil.getRootParentNode
import mega.privacy.android.app.utils.MegaNodeUtil.isVideo
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.GetThumbnail
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
    private val getThumbnail: GetThumbnail,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
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
    private val bucket: MutableStateFlow<MegaRecentActionBucket?> = MutableStateFlow(null)

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
    val items = bucket
        .map { it?.let { getNodes(it) } ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            monitorNodeUpdates().collectLatest {
                Timber.d("Received node update")
                updateCurrentBucket()
                clearSelection()
            }

            items.collectLatest {
                isInShare = it.firstOrNull()?.node?.let { node ->
                    megaApi.getRootParentNode(node).isInShare
                } ?: false
            }
        }
    }

    /**
     * Set bucket value
     *
     * @param selectedBucket
     */
    fun setBucket(selectedBucket: MegaRecentActionBucket?) {
        viewModelScope.launch {
            bucket.emit(selectedBucket)
        }
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
    private suspend fun updateCurrentBucket() = withContext(ioDispatcher) {
        val recentActions = megaApi.recentActions

        // Update the current bucket
        bucket.value?.let { currentBucket ->
            recentActions.firstOrNull { isSameBucket(it, currentBucket) }?.let {
                bucket.emit(it)
                return@withContext
            }
        }

        // Compare the list of recentActions with the new list of recent actions
        // and only keep the actions that differs from each other
        cachedActionList?.forEach { b ->
            val iterator = recentActions.iterator()
            while (iterator.hasNext()) {
                if (isSameBucket(iterator.next(), b)) {
                    iterator.remove()
                }
            }
        }

        // The last one is the changed one
        if (recentActions.size == 1) {
            bucket.emit(recentActions[0])
            return@withContext
        }

        // No nodes contained in the bucket or the action bucket is no loner exists.
        _shouldCloseFragment.postValue(true)
    }

    /**
     * Compare two MegaRecentActionBucket
     *
     * @param selected the first MegaRecentActionBucket to compare
     * @param other the second MegaRecentActionBucket to compare
     * @return true if the two MegaRecentActionBucket are the same
     */
    private fun isSameBucket(
        selected: MegaRecentActionBucket,
        other: MegaRecentActionBucket,
    ): Boolean {
        return selected.isMedia == other.isMedia &&
                selected.isUpdate == other.isUpdate &&
                selected.timestamp == other.timestamp &&
                selected.parentHandle == other.parentHandle &&
                selected.userEmail == other.userEmail
    }

    /**
     * Get the node from the bucket
     *
     * @return a list of NodeItem
     */
    private suspend fun getNodes(bucket: MegaRecentActionBucket): List<NodeItem> =
        withContext(ioDispatcher) {
            val size = bucket.nodes.size()
            val coroutineScope = CoroutineScope(SupervisorJob())
            val deferredNodeItems = mutableListOf<Deferred<NodeItem>>().apply {
                for (i in 0 until size) {
                    bucket.nodes[i]?.let { node ->
                        add(
                            coroutineScope.async {
                                NodeItem(
                                    node = node,
                                    thumbnail = getThumbnail(node.handle),
                                    index = -1,
                                    isVideo = node.isVideo(),
                                    modifiedDate = node.modificationTime.toString(),
                                )
                            }
                        )
                    }
                }
            }

            val nodesList = ArrayList<NodeItem>().apply {
                deferredNodeItems.forEach {
                    try {
                        add(it.await())
                    } catch (e: Exception) {
                        Timber.e(e)
                    }
                }

            }

            return@withContext nodesList
        }

}

