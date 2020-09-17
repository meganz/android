package mega.privacy.android.app.fragments.homepage.video

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import mega.privacy.android.app.fragments.homepage.NodeItem
import mega.privacy.android.app.fragments.homepage.TypedFilesRepository
import mega.privacy.android.app.fragments.homepage.nodesChange
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.TextUtil
import nz.mega.sdk.MegaApiJava.*

class VideoViewModel @ViewModelInject constructor(
    private val repository: TypedFilesRepository
) : ViewModel() {

    private var _query = MutableLiveData<String>()

    var order: Int = ORDER_DEFAULT_ASC
        private set

    var searchMode = false
    var searchQuery = ""

    private var forceUpdate = false
    private var ignoredFirstNodesChange = false

    // Whether a video loading is in progress
    private var loadInProgress = false

    // Whether another video loading should be executed after current loading
    private var pendingLoad = false

    val items: LiveData<List<NodeItem>> = _query.switchMap {
        if (forceUpdate) {
            viewModelScope.launch {
                repository.getFiles(NODE_VIDEO, order)
            }
        } else {
            repository.emitFiles()
        }

        repository.fileNodeItems
    }.map { nodes ->
        var index = 0
        val filteredNodes = ArrayList(
            if (!TextUtil.isTextEmpty(_query.value)) {
                nodes.filter {
                    it.node?.name?.contains(_query.value!!, true) ?: false
                }
            } else {
                nodes
            }
        )

        if (!searchMode && filteredNodes.isNotEmpty()) {
            filteredNodes.add(0, NodeItem.SORT_BY_HEADER)
        }

        filteredNodes.forEach {
            it.index = index++
        }

        filteredNodes
    }

    private val nodesChangeObserver = Observer<Boolean> {
        if (!ignoredFirstNodesChange) {
            ignoredFirstNodesChange = true
            return@Observer
        }

        if (it) {
            loadVideo(true)
        } else {
            refreshUi()
        }
    }

    private val loadFinishedObserver = Observer<List<NodeItem>> {
        loadInProgress = false

        if (pendingLoad) {
            loadVideo(true)
        }
    }

    init {
        items.observeForever(loadFinishedObserver)
        nodesChange.observeForever(nodesChangeObserver)
    }

    /**
     * Load video by calling Mega Api or just filter loaded nodes
     * @param forceUpdate True if retrieve all nodes by calling API,
     *                    false if filter current nodes by searchQuery
     */
    fun loadVideo(forceUpdate: Boolean = false, order: Int = this.order) {
        this.forceUpdate = forceUpdate
        this.order = order

        if (loadInProgress) {
            pendingLoad = true
        } else {
            pendingLoad = false
            loadInProgress = true
            _query.value = searchQuery
        }
    }

    /**
     * Make the list adapter to rebind all item views with data since
     * the underlying data may have been changed.
     */
    fun refreshUi() {
        items.value?.forEach { item ->
            item.uiDirty = true
        }
        loadVideo()
    }

    fun shouldShowSearchMenu() = items.value?.isNotEmpty() ?: false

    fun getNodePositionByHandle(handle: Long): Int {
        return items.value?.find {
            it.node?.handle == handle
        }?.index ?: Constants.INVALID_POSITION
    }

    fun getHandlesOfVideo(): LongArray? {
        val list = items.value?.map { node -> node.node?.handle ?: INVALID_HANDLE }

        return list?.toLongArray()
    }

    fun getRealNodeCount() = items.value?.size?.minus(if (searchMode) 0 else 1) ?: 0

    override fun onCleared() {
        nodesChange.removeObserver(nodesChangeObserver)
    }
}

