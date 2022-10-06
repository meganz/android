package mega.privacy.android.app.fragments.homepage.audio

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.app.domain.usecase.MonitorNodeUpdates
import mega.privacy.android.app.fragments.homepage.NodeItem
import mega.privacy.android.app.fragments.homepage.TypedFilesRepository
import mega.privacy.android.app.search.callback.SearchCallback
import mega.privacy.android.app.utils.Constants.EVENT_NODES_CHANGE
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaCancelToken
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel associated to [AudioFragment]
 *
 * @param repository
 * @param getCloudSortOrder
 * @param sortOrderIntMapper
 * @param monitorNodeUpdates
 */
@HiltViewModel
class AudioViewModel @Inject constructor(
    private val repository: TypedFilesRepository,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val sortOrderIntMapper: SortOrderIntMapper,
    monitorNodeUpdates: MonitorNodeUpdates,
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

    /**
     * Sort order used in the search function
     */
    private var sortOrder: SortOrder = SortOrder.ORDER_DEFAULT_ASC

    val items: LiveData<List<NodeItem>> = _query.switchMap {
        if (forceUpdate || repository.fileNodeItems.value == null) {
            viewModelScope.launch {
                cancelToken = initNewSearch()
                repository.getFiles(
                    cancelToken!!,
                    MegaApiJava.FILE_TYPE_AUDIO,
                    sortOrderIntMapper(sortOrder),
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

    private val nodesChangeObserver = Observer<Boolean> { forceUpdate ->
        if (!forceUpdate) {
            refreshUi()
        }
    }

    init {
        viewModelScope.launch {
            sortOrder = getCloudSortOrder()
            loadAudio(true)
        }

        items.observeForever(loadFinishedObserver)
        LiveEventBus.get(EVENT_NODES_CHANGE, Boolean::class.java)
            .observeForever(nodesChangeObserver)

        viewModelScope.launch {
            monitorNodeUpdates().collectLatest {
                Timber.d("Received node update")
                loadAudio(true)
            }
        }
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
