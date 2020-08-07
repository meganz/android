package mega.privacy.android.app.fragments.photos

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import kotlinx.coroutines.launch

class PhotosViewModel @ViewModelInject constructor(
    private val photosRepository: PhotosRepository
) : ViewModel() {

    private var count: Int = 0
    private var _query = MutableLiveData<Int>()
//    private var _items : LiveData<List<PhotoNode>>

    val items: LiveData<List<PhotoNode>> = Transformations.switchMap(_query) { query ->
            viewModelScope.launch {
                photosRepository.getPhotos(PhotoQuery(searchDate = LongArray(0)))
//            if (result is Result.Success) {
//                _items.value = result.data
//            } else {
//                logError(result.toString())
//            }
            }

            photosRepository.photoNodes
    }

    fun loadPhotos(query: PhotoQuery) {
        _query.value = 123
    }

    fun refresh() {
        _query.value?.let {
            _query.value = it
        }
    }
}