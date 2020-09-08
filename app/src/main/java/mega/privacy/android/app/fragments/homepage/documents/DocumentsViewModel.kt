package mega.privacy.android.app.fragments.homepage.documents

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import mega.privacy.android.app.fragments.homepage.NodeItem
import mega.privacy.android.app.fragments.homepage.TypedFilesRepository
import mega.privacy.android.app.fragments.homepage.nodesChange
import mega.privacy.android.app.fragments.homepage.photos.PhotoNodeItem
import mega.privacy.android.app.utils.TextUtil
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaApiJava.ORDER_DEFAULT_ASC

class DocumentsViewModel @ViewModelInject constructor(
    private val repository: TypedFilesRepository
) : ViewModel() {

    private var _query = MutableLiveData<String>("")

    private var order: Int = ORDER_DEFAULT_ASC
    var searchMode = false
    var listMode = true   // false for grid mode
    var searchQuery = ""

    private var forceUpdate = false
    private var ignoredFirst = false

    // Whether a photo loading is in progress
    private var loadInProgress = false
    // Whether another photo loading should be executed after current loading
    private var pendingLoad = false

    val items: LiveData<List<NodeItem>> = _query.switchMap {
        if (forceUpdate) {
            viewModelScope.launch {
                repository.getFiles(MegaApiJava.NODE_DOCUMENT, order)
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

    private val loadFinishedObserver = Observer<List<NodeItem>> {
        loadInProgress = false

        if (pendingLoad) {
            loadDocuments(true)
        }
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
        items.observeForever(loadFinishedObserver)
        nodesChange.observeForever(nodesChangeObserver)
    }

    /**
     * Load photos by calling Mega Api or just filter loaded nodes
     * @param forceUpdate True if retrieve all nodes by calling API
     * , false if filter current nodes by searchQuery
     */
    fun loadDocuments(forceUpdate: Boolean = false, order: Int = this.order) {
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
        items.value?.forEach {item ->
            item.uiDirty = true
        }
        loadDocuments()
    }

    fun shouldShowSearchMenu() = items.value?.isNotEmpty() ?: false

    override fun onCleared() {
        nodesChange.removeObserver(nodesChangeObserver)
        items.removeObserver(loadFinishedObserver)
    }
}