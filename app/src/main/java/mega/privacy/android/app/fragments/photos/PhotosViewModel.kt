package mega.privacy.android.app.fragments.photos

import androidx.lifecycle.*
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.launch
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import mega.privacy.android.app.utils.TextUtil
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import javax.inject.Inject

@ActivityRetainedScoped
// Use scoped @Inject instead of @ViewModelInject. Then the ViewModel object in PhotosFragment
// and PhotosGridAdapter are identical
class PhotosViewModel @Inject constructor(
    private val photosRepository: PhotosRepository
) : ViewModel() {

    private var _query = MutableLiveData<String>("")

    private val _openPhotoEvent = MutableLiveData<Event<PhotoNode>>()
    val openPhotoEvent: LiveData<Event<PhotoNode>> = _openPhotoEvent

    private val _showFileInfoEvent = MutableLiveData<Event<PhotoNode>>()
    val showFileInfoEvent: LiveData<Event<PhotoNode>> = _showFileInfoEvent

    var searchMode = false
    var searchQuery = ""

    private var forceUpdate = false

    val items: LiveData<List<PhotoNode>> = _query.switchMap {
        viewModelScope.launch {
            photosRepository.getPhotos(forceUpdate)
        }

        photosRepository.photoNodes
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

        filteredNodes.forEach {
            it.index = index++
            if (it.type == PhotoNode.TYPE_PHOTO) it.photoIndex = photoIndex++
        }

        filteredNodes
    }


    fun loadPhotos(query: String, forceUpdate: Boolean = false) {
        this.forceUpdate = forceUpdate
        searchQuery = query
        _query.value = query
    }

    fun onPhotoClick(item: PhotoNode) {
        _openPhotoEvent.value = Event(item)
    }

    fun getRealNodeCount(): Int {
        items.value?.filter { it.type == PhotoNode.TYPE_PHOTO }?.let {
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
            it.type == PhotoNode.TYPE_PHOTO
        }?.map { node -> node.node?.handle ?: INVALID_HANDLE }

        return list?.toLongArray()
    }

    fun showFileInfo(item: PhotoNode) {
        _showFileInfoEvent.value = Event(item)
    }
}