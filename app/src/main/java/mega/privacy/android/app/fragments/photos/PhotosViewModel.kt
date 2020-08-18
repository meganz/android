package mega.privacy.android.app.fragments.photos

import android.util.Log
import androidx.lifecycle.*
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.launch
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

    val items: LiveData<List<PhotoNode>> = Transformations.switchMap(_query) {
        viewModelScope.launch {
            photosRepository.getPhotos()
        }

        photosRepository.photoNodes
    }

    fun loadPhotos() {
        _query.value?.let {
            _query.value = it
        }
    }

    fun onPhotoClick(item: PhotoNode) {
        Log.i("Alex", "onClick:$this")
        _openPhotoEvent.value = Event(item)
    }
}