package mega.privacy.android.app.fragments.homepage.audio

import androidx.lifecycle.*
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import mega.privacy.android.app.fragments.homepage.NodeItem
import mega.privacy.android.app.fragments.homepage.TypedFilesRepository
import mega.privacy.android.app.globalmanagement.SortOrderManagement
import mega.privacy.android.app.search.callback.SearchCallback
import mega.privacy.android.app.utils.Constants.EVENT_NODES_CHANGE
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import mega.privacy.android.app.utils.TextUtil
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaCancelToken
import javax.inject.Inject

@HiltViewModel
class AudioViewModel @Inject constructor(
    private val repository: TypedFilesRepository,
    private val sortOrderManagement: SortOrderManagement
) : ViewModel(), SearchCallback.Data {

    private var _query = MutableLiveData<String>()

    var isList = true
    var skipNextAutoScroll = false
    var searchMode = false
    var searchQuery = ""

    private var forceUpdate = false

    // Whether a audio loading is in progress
    private var loadInProgress = false

    // Whether another audio loading should be executed after current loading
    private var pendingLoad = false

    private var cancelToken: MegaCancelToken? = null

    val items: LiveData<List<NodeItem>> = _query.switchMap {
        if (forceUpdate || repository.fileNodeItems.value == null) {
            viewModelScope.launch {
                cancelToken = initNewSearch()
                repository.getFiles(
                    cancelToken!!,
                    MegaApiJava.FILE_TYPE_AUDIO,
                    sortOrderManagement.getOrderCloud()
                )
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
                    it.node?.name?.contains(
                        _query.value!!,
                        true
                    ) ?: false
                }
            } else {
                nodes
            }
        )

        if (!searchMode && filteredNodes.isNotEmpty()) {
            filteredNodes.add(0, NodeItem())
        }

        filteredNodes.forEach {
            it.index = index++
        }

        filteredNodes
    }

    private val loadFinishedObserver = Observer<List<NodeItem>> {
        loadInProgress = false

        if (pendingLoad) {
            loadAudio(true)
        }
    }

    private val nodesChangeObserver = Observer<Boolean> {
        if (it) {
            loadAudio(true)
        } else {
            refreshUi()
        }
    }

    init {
        items.observeForever(loadFinishedObserver)
        LiveEventBus.get(EVENT_NODES_CHANGE, Boolean::class.java)
            .observeForever(nodesChangeObserver)
        loadAudio(true)
    }

    /**
     * Load audio by calling Mega Api or just filter loaded nodes
     * @param forceUpdate True if retrieve all nodes by calling API
     * , false if filter current nodes by searchQuery
     */
    fun loadAudio(forceUpdate: Boolean = false) {
        this.forceUpdate = forceUpdate

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
        loadAudio()
    }

    fun shouldShowSearchMenu() = items.value?.isNotEmpty() ?: false

    fun getNodePositionByHandle(handle: Long) =
        items.value?.find { it.node?.handle == handle }?.index ?: INVALID_POSITION

    fun getHandlesOfAudio() =
        items.value?.map { node -> node.node?.handle ?: INVALID_HANDLE }?.toLongArray()

    fun getRealNodeCount() = items.value?.size?.minus(if (searchMode) 0 else 1) ?: 0

    override fun onCleared() {
        LiveEventBus.get(EVENT_NODES_CHANGE, Boolean::class.java)
            .removeObserver(nodesChangeObserver)
        items.removeObserver(loadFinishedObserver)
    }

    override fun initNewSearch(): MegaCancelToken {
        cancelSearch()
        return MegaCancelToken.createInstance()
    }

    override fun cancelSearch() {
        cancelToken?.cancel()
    }
}
