package mega.privacy.android.app.fragments.homepage.photos

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.launch
import mega.privacy.android.app.fragments.homepage.TypedFilesRepository
import mega.privacy.android.app.fragments.homepage.nodesChange
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import mega.privacy.android.app.utils.TextUtil
import nz.mega.sdk.MegaApiJava.*

class PhotosViewModel @ViewModelInject constructor(
    private val repository: TypedFilesRepository
) : ViewModel() {

    private var _query = MutableLiveData<String>()

    var searchMode = false
    var searchQuery = ""
    var skipNextAutoScroll = false

    private var forceUpdate = false

    // Whether a photo loading is in progress
    private var loadInProgress = false

    // Whether another photo loading should be executed after current loading
    private var pendingLoad = false

    val items: LiveData<List<PhotoNodeItem>> = _query.switchMap {
        if (forceUpdate) {
            viewModelScope.launch {
                repository.getFiles(FILE_TYPE_PHOTO, ORDER_MODIFICATION_DESC)
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

        if (searchMode) {
            filteredNodes = filteredNodes.filter {
                it.type == PhotoNodeItem.TYPE_PHOTO
            }
        }

        if (!TextUtil.isTextEmpty(_query.value)) {
            filteredNodes = items.filter {
                it.node?.name?.contains(
                    _query.value!!,
                    true
                ) ?: false
            }
        }

        filteredNodes.forEach {
            it.index = index++
            if (it.type == PhotoNodeItem.TYPE_PHOTO) it.photoIndex = photoIndex++
        }

        filteredNodes
    }

    private val nodesChangeObserver = Observer<Boolean> {
        if (it) {
            loadPhotos(true)
        } else {
            refreshUi()
        }
    }

    private val loadFinishedObserver = Observer<List<PhotoNodeItem>> {
        loadInProgress = false

        if (pendingLoad) {
            loadPhotos(true)
        }
    }

    init {
        items.observeForever(loadFinishedObserver)
        // Calling ObserveForever() here instead of calling observe()
        // in the PhotosFragment, for fear that an nodes update event would be missed if
        // emitted accidentally between the Fragment's onDestroy and onCreate when rotating screen.
        LiveEventBus.get(Constants.EVENT_NODES_CHANGE, Boolean::class.java)
            .observeForever(nodesChangeObserver)
        loadPhotos(true)
    }

    /**
     * Load photos by calling Mega Api or just filter loaded nodes
     * @param forceUpdate True if retrieve all nodes by calling API
     * , false if filter current nodes by searchQuery
     */
    fun loadPhotos(forceUpdate: Boolean = false) {
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
     * the underlying meta data of items may have been changed.
     */
    fun refreshUi() {
        items.value?.forEach { item ->
            item.uiDirty = true
        }
        loadPhotos()
    }

    fun getRealPhotoCount(): Int {
        items.value?.filter { it.type == PhotoNodeItem.TYPE_PHOTO }?.let {
            return it.size
        }

        return 0
    }

    fun shouldShowSearchMenu() = items.value?.isNotEmpty() ?: false

    fun getItemPositionByHandle(handle: Long) =
        items.value?.find { it.node?.handle == handle }?.index ?: INVALID_POSITION

    fun getHandlesOfPhotos(): LongArray? {
        val list = items.value?.filter {
            it.type == PhotoNodeItem.TYPE_PHOTO
        }?.map { node -> node.node?.handle ?: INVALID_HANDLE }

        return list?.toLongArray()
    }

    override fun onCleared() {
        LiveEventBus.get(Constants.EVENT_NODES_CHANGE, Boolean::class.java)
            .removeObserver(nodesChangeObserver)
        items.removeObserver(loadFinishedObserver)
    }
}