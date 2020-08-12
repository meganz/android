package mega.privacy.android.app.fragments.photos

import android.util.Log
import androidx.lifecycle.*
import dagger.hilt.android.scopes.FragmentScoped
import kotlinx.coroutines.launch
import javax.inject.Inject

@FragmentScoped
// Use scoped @Inject instead of @ViewModelInject. Then the ViewModel object in PhotosFragment
// and PhotosGridAdapter are identical
class PhotosViewModel @Inject constructor(
    private val photosRepository: PhotosRepository
) : ViewModel() {

    private var _query = MutableLiveData<PhotoQuery>()

    val items: LiveData<List<PhotoNode>> = Transformations.switchMap(_query) { query ->
        viewModelScope.launch {
            photosRepository.getPhotos(query)
        }

        photosRepository.photoNodes
    }

    fun loadPhotos(query: PhotoQuery) {
        _query.value = query
    }

    fun refresh() {
        _query.value?.let {
            _query.value = it
        }
    }

    fun onPhotoClick() {
        Log.i("Alex", "onClick:$this")
        // TODO: Navigate to Photo Viewer
    }
}