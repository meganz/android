package mega.privacy.android.app.fragments.homepage.photos

import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import mega.privacy.android.app.fragments.homepage.Event
import mega.privacy.android.app.fragments.homepage.ItemOperation
import mega.privacy.android.app.fragments.homepage.NodeItem
import mega.privacy.android.app.fragments.homepage.nodesChange
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import mega.privacy.android.app.utils.TextUtil
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE

class PhotosViewModel @ViewModelInject constructor(
    private val photosRepository: PhotosRepository
) : ViewModel(), ItemOperation {

    private var _query = MutableLiveData<String>("")

    private val _openPhotoEvent = MutableLiveData<Event<PhotoNodeItem>>()
    val openPhotoEventItem: LiveData<Event<PhotoNodeItem>> = _openPhotoEvent

    private val _showNodeOptionsEvent = MutableLiveData<Event<NodeItem>>()
    val showNodeItemOptionsEvent: LiveData<Event<NodeItem>> = _showNodeOptionsEvent

    var searchMode = false
    var searchQuery = ""

    private var forceUpdate = false
    private var ignoredFirst = false

    val items: LiveData<List<PhotoNodeItem>> = _query.switchMap {
        viewModelScope.launch {
            photosRepository.getPhotos(forceUpdate)
        }

        photosRepository.photoNodesItem
    }.map { nodes ->
        var index = 0
        var photoIndex = 0
        var filteredNodes = nodes

        if (!TextUtil.isTextEmpty(_query.value)) {
            filteredNodes = nodes.filter {
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
        items.value?.forEach {photoNode ->
            photoNode.uiDirty = true
        }
        loadPhotos()
    }

    override fun onItemClick(item: NodeItem) {
        _openPhotoEvent.value = Event(item as PhotoNodeItem)
    }

    fun getRealNodeCount(): Int {
        items.value?.filter { it.type == PhotoNodeItem.TYPE_PHOTO }?.let {
            return it.size
        }

        return 0
    }

    fun shouldShowSearchMenu() = items.value?.isNotEmpty() ?: false

    fun getNodePositionByHandle(handle: Long): Int {
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

    override fun showNodeOptionsInfo(item: NodeItem) {
        _showNodeOptionsEvent.value = Event(item)
    }

    override fun onCleared() {
        nodesChange.removeObserver(nodesChangeObserver)
    }
}