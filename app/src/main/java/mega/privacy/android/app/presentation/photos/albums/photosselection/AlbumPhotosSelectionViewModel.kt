package mega.privacy.android.app.presentation.photos.albums.photosselection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.presentation.photos.albums.photosselection.AlbumPhotosSelectionActivity.Companion.ALBUM_ID
import mega.privacy.android.app.presentation.photos.timeline.model.TimelinePhotosSource
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.usecase.GetUserAlbum
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class AlbumPhotosSelectionViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getUserAlbum: GetUserAlbum,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _state = MutableStateFlow(AlbumPhotosSelectionState())
    val state: StateFlow<AlbumPhotosSelectionState> = _state

    init {
        fetchAlbum()
    }

    private fun fetchAlbum() = savedStateHandle.getStateFlow<Long?>(ALBUM_ID, null)
        .filterNotNull()
        .flatMapLatest { id -> getUserAlbum(albumId = AlbumId(id)) }
        .filterNotNull()
        .map(::updateAlbum)
        .catch { exception -> Timber.e(exception) }
        .launchIn(viewModelScope)

    private fun updateAlbum(album: Album.UserAlbum) {
        _state.update {
            it.copy(album = album)
        }
    }

    fun updateLocation(location: TimelinePhotosSource) {
        _state.update {
            it.copy(selectedLocation = location)
        }
    }

    fun selectAllPhotos() = viewModelScope.launch {
        _state.update {
            val selectedPhotoIds = withContext(defaultDispatcher) {
                it.photos.map { photo -> photo.id }.toSet()
            }
            it.copy(selectedPhotoIds = selectedPhotoIds)
        }
    }

    fun clearSelection() {
        _state.update {
            it.copy(selectedPhotoIds = setOf())
        }
    }
}
