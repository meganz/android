package mega.privacy.android.app.fragments.recent

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.fragments.homepage.NodeItem
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaRecentActionBucket
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel associated to [RecentsBucketFragment]
 */
@HiltViewModel
class RecentsBucketViewModel @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val recentsBucketRepository: RecentsBucketRepository,
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
    private val bucket: MutableLiveData<MegaRecentActionBucket> = MutableLiveData()

    private var cachedActionList: List<MegaRecentActionBucket>? = null

    private val _shouldCloseFragment: MutableLiveData<Boolean> = MutableLiveData(false)

    /**
     * True if the fragment needs to be closed
     */
    val shouldCloseFragment: LiveData<Boolean> = _shouldCloseFragment

    /**
     *  List of node items in the current bucket
     */
    val items: LiveData<List<NodeItem>> = bucket.switchMap {
        viewModelScope.launch {
            recentsBucketRepository.getNodes(it)
        }

        recentsBucketRepository.nodes
    }

    init {
        viewModelScope.launch {
            monitorNodeUpdates().collectLatest {
                Timber.d("Received node update")
                updateCurrentBucket()
            }
        }
    }

    /**
     * Set bucket value
     *
     * @param selectedBucket
     */
    fun setBucket(selectedBucket: MegaRecentActionBucket?) {
        bucket.value = selectedBucket
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
    fun getNodesCount(): Int = items.value?.size ?: 0

    /**
     * Clear selected nodes
     */
    fun clearSelection() {
        _actionMode.value = false
        selectedNodes.clear()

        val animNodeIndices = mutableSetOf<Int>()
        val nodeList = items.value ?: return

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

        if (nodeList == null || position < 0 || position >= nodeList.size
            || nodeList[position].hashCode() != node.hashCode()
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
        val nodeList = items.value ?: return

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
    private fun updateCurrentBucket() {
        val recentActions = megaApi.recentActions

        // Update the current bucket
        bucket.value?.let { currentBucket ->
            recentActions.firstOrNull { isSameBucket(it, currentBucket) }?.let {
                bucket.value = it
                return
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
            bucket.value = recentActions[0]
            return
        }

        // No nodes contained in the bucket or the action bucket is no loner exists.
        _shouldCloseFragment.value = true
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

}

