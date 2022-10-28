package mega.privacy.android.app.presentation.photos.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.GetNodeListByIds
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.photos.albums.model.AlbumsViewState
import mega.privacy.android.app.presentation.photos.albums.model.UIAlbum
import mega.privacy.android.app.presentation.photos.albums.model.getAlbumPhotos
import mega.privacy.android.app.presentation.photos.albums.model.mapper.UIAlbumMapper
import mega.privacy.android.app.presentation.photos.model.Sort
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.entity.photos.PhotoPredicate
import mega.privacy.android.domain.usecase.GetDefaultAlbumPhotos
import mega.privacy.android.domain.usecase.GetDefaultAlbumsMap
import mega.privacy.android.domain.usecase.GetFeatureFlagValue
import mega.privacy.android.domain.usecase.RemoveFavourites
import timber.log.Timber
import javax.inject.Inject

/**
 * AlbumsViewModel handle albums cover page view logic
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AlbumsViewModel @Inject constructor(
    private val getDefaultAlbumPhotos: GetDefaultAlbumPhotos,
    private val getDefaultAlbumsMap: GetDefaultAlbumsMap,
    private val uiAlbumMapper: UIAlbumMapper,
    private var getFeatureFlag: GetFeatureFlagValue,
    private val removeFavourites: RemoveFavourites,
    private val getNodeListByIds: GetNodeListByIds,
) : ViewModel() {

    private val _state = MutableStateFlow(AlbumsViewState())
    val state = _state.asStateFlow()
    private var currentNodeJob: Job? = null

    internal val selectedPhotoIds = mutableSetOf<Long>()

    private suspend fun getSystemAlbums(): Map<Album, PhotoPredicate> {
        val albums = getDefaultAlbumsMap()
        return if (getFeatureFlag(AppFeatures.DynamicAlbum)) {
            albums
        } else {
            albums.filter { it.key is Album.FavouriteAlbum }
        }
    }

    init {
        loadAlbums()
    }

    private fun loadAlbums() {
        currentNodeJob = viewModelScope.launch {
            val includedSystemAlbums = getSystemAlbums()
            runCatching {
                getDefaultAlbumPhotos(
                    includedSystemAlbums.values.toList()
                ).mapLatest { photos ->
                    includedSystemAlbums.mapNotNull { (key, value) ->
                        photos.filter { value(it) }.takeIf { shouldAddAlbum(it, key) }
                            ?.let { uiAlbumMapper(it, key) }
                    }
                }.collectLatest { albums ->
                    val currentAlbumId = checkCurrentAlbumExists(albums = albums)
                    _state.update {
                        it.copy(
                            albums = albums,
                            currentAlbumId = currentAlbumId
                        )
                    }
                }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    private fun checkCurrentAlbumExists(albums: List<UIAlbum>): Album? =
        albums.find { uiAlbum -> uiAlbum.id == _state.value.currentAlbumId }?.id


    private fun shouldAddAlbum(
        it: List<Photo>,
        key: Album,
    ) = it.isNotEmpty() || key == Album.FavouriteAlbum


    fun setCurrentAlbum(album: Album?) {
        _state.update {
            it.copy(currentAlbumId = album)
        }
    }

    fun togglePhotoSelection(id: Long) {
        if (id in selectedPhotoIds) {
            selectedPhotoIds.remove(id)
        } else {
            selectedPhotoIds.add(id)
        }
        _state.update {
            it.copy(selectedPhotoIds = selectedPhotoIds.toMutableSet())
        }
    }

    fun clearSelectedPhotos() {
        selectedPhotoIds.clear()
        _state.update {
            it.copy(selectedPhotoIds = selectedPhotoIds.toMutableSet())
        }
    }

    fun selectAllPhotos() {
        _state.value.currentAlbumId?.let { album ->
            val albumPhotosHandles =
                _state.value.albums.getAlbumPhotos(album).map { photo ->
                    photo.id
                }
            selectedPhotoIds.clear()
            selectedPhotoIds.addAll(albumPhotosHandles)
            _state.update {
                it.copy(selectedPhotoIds = selectedPhotoIds.toMutableSet())
            }
        }
    }

    fun removeFavourites() {
        viewModelScope.launch {
            removeFavourites(selectedPhotoIds.toList())
        }
        selectedPhotoIds.clear()
        _state.update {
            it.copy(selectedPhotoIds = selectedPhotoIds.toMutableSet())
        }
    }

    suspend fun getSelectedNodes() =
        getNodeListByIds(selectedPhotoIds.toList())

    fun setCurrentSort(sort: Sort) {
        _state.update {
            it.copy(currentSort = sort)
        }
    }
}
