package mega.privacy.android.app.fragments.homepage.documents

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import mega.privacy.android.app.fragments.homepage.*
import mega.privacy.android.app.fragments.homepage.photos.PhotoNodeItem
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import mega.privacy.android.app.utils.TextUtil
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE

class DocumentsViewModel @ViewModelInject constructor(
    private val repository: TypedFilesRepository
) : ViewModel(), ItemOperation {

    private var _query = MutableLiveData<String>("")

    private val _openDocEvent = MutableLiveData<Event<NodeItem>>()
    val openDocEvent: LiveData<Event<NodeItem>> = _openDocEvent

    private val _showNodeOptionsEvent = MutableLiveData<Event<NodeItem>>()
    val showNodeItemOptionsEvent: LiveData<Event<NodeItem>> = _showNodeOptionsEvent

    var searchMode = false
    var listMode = true   // false for grid mode
    var searchQuery = ""

    private var forceUpdate = false
    private var ignoredFirst = false

    val items: LiveData<List<NodeItem>> = _query.switchMap {
        if (forceUpdate) {
            viewModelScope.launch {
                repository.getFiles(MegaApiJava.NODE_DOCUMENT)
            }
        } else {
            repository.emitFiles()
        }

        repository.fileNodeItems
    }.map { nodes ->
        var index = 0
        var filteredNodes = nodes

        if (!TextUtil.isTextEmpty(_query.value)) {
            filteredNodes = nodes.filter {
                it.node?.name?.contains(
                    _query.value!!,
                    true
                ) ?: false
            }
        }

        filteredNodes.forEach {
            it.index = index++
        }

        filteredNodes
    }

    private val nodesChangeObserver = Observer<Boolean> {
        if (!ignoredFirst) {
            ignoredFirst = true
            return@Observer
        }

        if (it) {
            loadDocuments(true)
        } else {
            refreshUi()
        }
    }

    init {
        loadDocuments(true)
        nodesChange.observeForever(nodesChangeObserver)
    }

    /**
     * Load photos by calling Mega Api or just filter loaded nodes
     * @param forceUpdate True if retrieve all nodes by calling API
     * , false if filter current nodes by searchQuery
     */
    fun loadDocuments(forceUpdate: Boolean = false) {
        this.forceUpdate = forceUpdate
        _query.value = searchQuery
    }

    /**
     * Make the list adapter to rebind all item views with data since
     * the underlying data may have been changed.
     */
    fun refreshUi() {
        items.value?.forEach {item ->
            item.uiDirty = true
        }
        loadDocuments()
    }

    override fun onItemClick(item: NodeItem) {
        _openDocEvent.value = Event(item as PhotoNodeItem)
    }

    fun shouldShowSearchMenu() = items.value?.isNotEmpty() ?: false

    fun getNodePositionByHandle(handle: Long): Int {
        return items.value?.find {
            it.node?.handle == handle
        }?.index ?: INVALID_POSITION
    }

    fun getHandlesOfPhotos(): LongArray? {
        val list = items.value?.map { node -> node.node?.handle ?: INVALID_HANDLE }

        return list?.toLongArray()
    }

    override fun showNodeItemOptions(item: NodeItem) {
        _showNodeOptionsEvent.value = Event(item)
    }

    override fun onCleared() {
        nodesChange.removeObserver(nodesChangeObserver)
    }
}