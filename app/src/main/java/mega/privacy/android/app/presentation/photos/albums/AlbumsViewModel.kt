package mega.privacy.android.app.presentation.photos.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.R
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
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.usecase.CreateAlbum
import mega.privacy.android.domain.usecase.GetAlbumPhotos
import mega.privacy.android.domain.usecase.GetDefaultAlbumPhotos
import mega.privacy.android.domain.usecase.GetDefaultAlbumsMap
import mega.privacy.android.domain.usecase.GetFeatureFlagValue
import mega.privacy.android.domain.usecase.GetUserAlbums
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
    private val getUserAlbums: GetUserAlbums,
    private val getAlbumPhotos: GetAlbumPhotos,
    private val uiAlbumMapper: UIAlbumMapper,
    private var getFeatureFlag: GetFeatureFlagValue,
    private val removeFavourites: RemoveFavourites,
    private val getNodeListByIds: GetNodeListByIds,
    private val createAlbum: CreateAlbum,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _state = MutableStateFlow(AlbumsViewState())
    val state = _state.asStateFlow()
    private var currentNodeJob: Job? = null

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
        loadUserAlbums()
    }

    private fun loadAlbums() {
        currentNodeJob = viewModelScope.launch {
            val includedSystemAlbums = getSystemAlbums()
            runCatching {
                getDefaultAlbumPhotos(
                    includedSystemAlbums.values.toList()
                ).mapLatest { photos ->
                    includedSystemAlbums.mapNotNull { (key, value) ->
                        photos.filter {
                            value(it)
                        }.takeIf {
                            shouldAddAlbum(it, key)
                        }?.let {
                            uiAlbumMapper(it, key)
                        }
                    }
                }.collectLatest { systemAlbums ->
                    _state.update { state ->
                        val albums = withContext(defaultDispatcher) {
                            val userAlbums = state.albums.filter { it.id is Album.UserAlbum }
                            systemAlbums + userAlbums
                        }
                        val currentAlbum = checkCurrentAlbumExists(albums = albums)
                        state.copy(
                            albums = albums,
                            currentAlbum = currentAlbum,
                        )
                    }
                }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    private fun loadUserAlbums() {
        viewModelScope.launch {
            if (!getFeatureFlag(AppFeatures.UserAlbums)) return@launch

            getUserAlbums()
                .catch { exception ->
                    Timber.e(exception)
                }.collectLatest { albums ->
                    albums.forEach(::fetchAlbumPhotos)
                }
        }
    }

    private fun fetchAlbumPhotos(album: Album.UserAlbum) {
        viewModelScope.launch {
            getAlbumPhotos(album.id)
                .catch { exception ->
                    Timber.e(exception)
                }.collectLatest { photos ->
                    processUserAlbum(album, photos)
                }
        }
    }

    private suspend fun processUserAlbum(album: Album.UserAlbum, photos: List<Photo>) {
        _state.update { state ->
            val albums = updateUIAlbums(state.albums, album, photos)
            val currentAlbumId = checkCurrentAlbumExists(albums = albums)

            state.copy(
                albums = albums,
                currentAlbum = currentAlbumId,
            )
        }
    }

    private suspend fun updateUIAlbums(
        albums: List<UIAlbum>,
        userAlbum: Album.UserAlbum,
        photos: List<Photo>,
    ): List<UIAlbum> = withContext(defaultDispatcher) {
        val (systemAlbums, userAlbums) = albums.partition {
            it.id !is Album.UserAlbum
        }
        val isReplaceAlbum = userAlbums.any {
            (it.id as? Album.UserAlbum)?.id == userAlbum.id
        }

        val updatedUserAlbums = if (isReplaceAlbum) {
            userAlbums.map {
                if ((it.id as? Album.UserAlbum)?.id == userAlbum.id) {
                    uiAlbumMapper(photos, userAlbum)
                } else {
                    it
                }
            }
        } else {
            userAlbums + uiAlbumMapper(photos, userAlbum)
        }.sortedByDescending { (it.id as? Album.UserAlbum)?.modificationTime }

        systemAlbums + updatedUserAlbums
    }

    /**
     * Create a new album
     *
     * @param title the name of the album
     */
    fun createNewAlbum(title: String) = viewModelScope.launch {
        try {
            if (checkTitleValidity(title)) {
                val finalName = title.ifEmpty {
                    _state.value.createAlbumPlaceholderTitle
                }
                val album = createAlbum(finalName)
                _state.update {
                    it.copy(currentAlbum = album)
                }
                Timber.d("Current album: ${album.title}")
            }
        } catch (exception: Exception) {
            Timber.e(exception)
        }
    }


    private fun checkCurrentAlbumExists(albums: List<UIAlbum>): Album? =
        albums.find { uiAlbum -> uiAlbum.id == _state.value.currentAlbum }?.id

    /**
     * Get the default album title
     */
    fun setPlaceholderAlbumTitle(placeholderTitle: String) {
        val allUserAlbumsTitle: List<String> = getAllUserAlbumsNames()
        var i = 0
        var currentDefaultTitle = placeholderTitle

        while (currentDefaultTitle in allUserAlbumsTitle) {
            currentDefaultTitle = placeholderTitle + "(${++i})"
        }

        _state.update {
            it.copy(createAlbumPlaceholderTitle = currentDefaultTitle)
        }
    }

    private fun getAllUserAlbumsNames() = _state.value.albums.filter {
        it.id is Album.UserAlbum
    }.map { it.title }

    private fun getAllSystemAlbumsNames() = _state.value.albums.filter {
        it.id !is Album.UserAlbum
    }.map { it.title }

    private fun checkTitleValidity(title: String): Boolean {
        var errorMessage: Int? = null
        var isTitleValid = true

        if (title in getAllSystemAlbumsNames()) {
            isTitleValid = false
            errorMessage = R.string.photos_create_album_error_message_systems_album
        } else if (title in getAllUserAlbumsNames()) {
            isTitleValid = false
            errorMessage = R.string.photos_create_album_error_message_duplicate
        } else if ("[*/:<>?\"|]".toRegex().containsMatchIn(title)) {
            isTitleValid = false
            errorMessage = R.string.invalid_characters_defined
        }

        _state.update {
            it.copy(
                isInputNameValid = isTitleValid,
                createDialogErrorMessage = errorMessage,
            )
        }

        return isTitleValid
    }

    fun setNewAlbumNameValidity(valid: Boolean) = _state.update {
        it.copy(isInputNameValid = valid)
    }

    private fun shouldAddAlbum(
        it: List<Photo>,
        key: Album,
    ) = it.isNotEmpty() || key == Album.FavouriteAlbum


    fun setCurrentAlbum(album: Album?) {
        _state.update {
            it.copy(currentAlbum = album)
        }
    }

    fun togglePhotoSelection(id: Long) {
        val selectedPhotoIds = _state.value.selectedPhotoIds.toMutableSet()
        if (id in selectedPhotoIds) {
            selectedPhotoIds.remove(id)
        } else {
            selectedPhotoIds.add(id)
        }
        _state.update {
            it.copy(selectedPhotoIds = selectedPhotoIds)
        }
    }

    fun clearSelectedPhotos() {
        _state.update {
            it.copy(selectedPhotoIds = emptySet())
        }
    }

    fun selectAllPhotos() {
        _state.value.currentAlbum?.let { album ->
            val albumPhotosHandles =
                _state.value.albums.getAlbumPhotos(album).map { photo ->
                    photo.id
                }
            _state.update {
                it.copy(selectedPhotoIds = albumPhotosHandles.toMutableSet())
            }
        }
    }

    fun getAlbumPhotosCount() =
        _state.value.albums.find { it.id == _state.value.currentAlbum }?.count ?: 0

    fun removeFavourites() {
        viewModelScope.launch {
            removeFavourites(_state.value.selectedPhotoIds.toList())
        }
        _state.update {
            it.copy(selectedPhotoIds = emptySet())
        }
    }

    suspend fun getSelectedNodes() =
        getNodeListByIds(_state.value.selectedPhotoIds.toList())

    fun setCurrentSort(sort: Sort) {
        _state.update {
            it.copy(currentSort = sort)
        }
    }
}
