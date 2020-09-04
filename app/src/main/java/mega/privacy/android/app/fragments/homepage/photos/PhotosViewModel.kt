package mega.privacy.android.app.fragments.homepage.photos

import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import mega.privacy.android.app.fragments.homepage.*
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import mega.privacy.android.app.utils.TextUtil
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaApiJava.NODE_PHOTO

class PhotosViewModel @ViewModelInject constructor(
    private val repository: TypedFilesRepository
) : ViewModel(), ItemOperation {

    private var _query = MutableLiveData<String>("")

    private val _openPhotoEvent = MutableLiveData<Event<PhotoNodeItem>>()
    val openPhotoEventItem: LiveData<Event<PhotoNodeItem>> = _openPhotoEvent

    private val _showNodeItemOptionsEvent = MutableLiveData<Event<NodeItem>>()
    val showNodeItemOptionsEvent: LiveData<Event<NodeItem>> = _showNodeItemOptionsEvent

    var searchMode = false
    var searchQuery = ""

    private var forceUpdate = false
    private var ignoredFirst = false

    val items: LiveData<List<PhotoNodeItem>> = _query.switchMap {
        if (forceUpdate) {
            viewModelScope.launch {
                repository.getFiles(NODE_PHOTO)
            }
        } else {
            repository.emitFiles()
        }

        repository.fileNodeItems
    }.map { it ->
        @Suppress("UNCHECKED_CAST")
        val items = it as List<PhotoNodeItem>
        var index = 0
        var photoIndex = 0
        var filteredNodes = items

        if (!TextUtil.isTextEmpty(_query.value)) {
            filteredNodes = items.filter {
                it.node?.name?.contains(
                    _query.value!!,
                    true
                ) ?: false
            }
        }

        if (searchMode) {
            filteredNodes = filteredNodes.filter {
                it.type == PhotoNodeItem.TYPE_PHOTO
            }
        }

        filteredNodes.forEach {
            it.index = index++
            if (it.type == PhotoNodeItem.TYPE_PHOTO) it.photoIndex = photoIndex++
        }

        filteredNodes
    }

    private val nodesChangeObserver = Observer<Boolean> {
        if (!ignoredFirst) {
            ignoredFirst = true
            return@Observer
        }

        if (it) {
            loadPhotos(true)
        } else {
            refreshUi()
        }
    }

    init {
        loadPhotos(true)
        nodesChange.observeForever(nodesChangeObserver)
    }

    /**
     * Load photos by calling Mega Api or just filter loaded nodes
     * @param forceUpdate True if retrieve all nodes by calling API
     * , false if filter current nodes by searchQuery
     */
    fun loadPhotos(forceUpdate: Boolean = false) {
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
        loadPhotos()
    }

    override fun onItemClick(item: NodeItem) {
        _openPhotoEvent.value = Event(item as PhotoNodeItem)
    }

    fun getRealPhotoCount(): Int {
        items.value?.filter { it.type == PhotoNodeItem.TYPE_PHOTO }?.let {
            return it.size
        }

        return 0
    }

    fun shouldShowSearchMenu() = items.value?.isNotEmpty() ?: false

    fun getItemPositionByHandle(handle: Long): Int {
        return items.value?.find {
            it.node?.handle == handle
        }?.index ?: INVALID_POSITION
    }

    fun getHandlesOfPhotos(): LongArray? {
        val list = items.value?.filter {
            it.type == PhotoNodeItem.TYPE_PHOTO
        }?.map { node -> node.node?.handle ?: INVALID_HANDLE }

        return list?.toLongArray()
    }

    override fun showNodeItemOptions(item: NodeItem) {
        _showNodeItemOptionsEvent.value = Event(item)
    }

    override fun onCleared() {
        nodesChange.removeObserver(nodesChangeObserver)
    }
}