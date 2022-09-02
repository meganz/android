package mega.privacy.android.app.fragments.recent

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.fragments.homepage.NodeItem
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
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
    private val _nodesToAnimate = MutableLiveData<Set<Int>>()

    val actionMode: LiveData<Boolean> = _actionMode
    val nodesToAnimate: LiveData<Set<Int>> = _nodesToAnimate

    private val selectedNodes: MutableSet<NodeItem> = mutableSetOf()

    var bucket: MutableLiveData<MegaRecentActionBucket> = MutableLiveData()

    var cachedActionList = MutableLiveData<List<MegaRecentActionBucket>>()

    var shouldCloseFragment: MutableLiveData<Boolean> = MutableLiveData(false)

    var items: LiveData<List<NodeItem>> = bucket.switchMap {
        viewModelScope.launch {
            recentsBucketRepository.getNodes(it)
        }

        recentsBucketRepository.nodes
    }

    var loadNodesJob: Job? = null

    fun getItemPositionByHandle(handle: Long): Int {
        var index = INVALID_POSITION

        items.value?.forEachIndexed { i, nodeItem ->
            if (nodeItem.node?.handle == handle) {
                index = i
                return@forEachIndexed
            }
        }

        return index
    }

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

    init {
        viewModelScope.launch {
            monitorNodeUpdates().collectLatest {
                Timber.d("Received node update")
                updateBucket()
            }
        }
    }

    fun getSelectedNodes(): List<NodeItem> = selectedNodes.toList()

    fun getSelectedNodesCount(): Int = selectedNodes.size

    fun getNodesCount(): Int = items.value?.size ?: 0

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
     * Update the bucket
     */
    private fun updateBucket() {
        if (bucket.value == null) {
            return
        }

        val recentActions = megaApi.recentActions

        recentActions.forEach { b ->
            bucket.value?.let { current ->
                if (isSameBucket(current, b)) {
                    bucket.value = b
                    return
                }
            }
        }

        cachedActionList.value?.forEach { b ->
            val iterator = recentActions.iterator()
            while (iterator.hasNext()) {
                if (isSameBucket(iterator.next(), b)) {
                    iterator.remove()
                }
            }
        }

        // The last one is the changed one.
        if (recentActions.size == 1) {
            bucket.value = recentActions[0]
            return
        }

        // No nodes contained in the bucket or the action bucket is no loner exists.
        shouldCloseFragment.value = true
    }

}

