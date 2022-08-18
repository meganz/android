package mega.privacy.android.app.fragments.recent

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.fragments.homepage.NodeItem
import mega.privacy.android.app.utils.Constants.EVENT_NODES_CHANGE
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaRecentActionBucket
import javax.inject.Inject

@HiltViewModel
class RecentsBucketViewModel @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val recentsBucketRepository: RecentsBucketRepository,
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

    private val nodesChangeObserver = Observer<Boolean> {
        if (it) {
            if (bucket.value == null) {
                return@Observer
            }

            val recentActions = megaApi.recentActions

            recentActions.forEach { b ->
                bucket.value?.let { current ->
                    if (isSameBucket(current, b)) {
                        bucket.value = b
                        return@Observer
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
                return@Observer
            }

            // No nodes contained in the bucket or the action bucket is no loner exists.
            shouldCloseFragment.value = true
        }
    }

    init {
        LiveEventBus.get(EVENT_NODES_CHANGE, Boolean::class.java)
            .observeForever(nodesChangeObserver)
    }

    override fun onCleared() {
        LiveEventBus.get(EVENT_NODES_CHANGE, Boolean::class.java)
            .removeObserver(nodesChangeObserver)
    }

    fun getSelectedNodes(): List<NodeItem> {
        return selectedNodes.toList()
    }

    fun getSelectedNodesCount(): Int {
        return selectedNodes.size
    }

    fun getNodesCount(): Int {
        return items.value?.size ?: 0
    }

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

    fun addOrRemoveSelectedItems(position: Int, node: MegaNode) {
        TODO("Not yet implemented")
    }
}

