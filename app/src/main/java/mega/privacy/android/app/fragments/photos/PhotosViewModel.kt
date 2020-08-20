package mega.privacy.android.app.fragments.photos

import androidx.lifecycle.*
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.launch
import mega.privacy.android.app.utils.TextUtil
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

    var searchMode = false

    private var forceUpdate = false

    private var index = 0
    private var photoIndex = 0

    val items: LiveData<List<PhotoNode>> = _query.switchMap {
        viewModelScope.launch {
            photosRepository.getPhotos(forceUpdate)
        }

        photosRepository.photoNodes
    }.map { nodes ->
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
        index = 0
        photoIndex = 0
        _query.value = query
    }

    fun onPhotoClick(item: PhotoNode) {
        _openPhotoEvent.value = Event(item)
    }
}