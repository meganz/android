package mega.privacy.android.app.presentation.photos.albums.getlink

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.presentation.photos.albums.AlbumScreenWrapperActivity.Companion.ALBUM_ID
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.DownloadThumbnail
import mega.privacy.android.domain.usecase.GetAlbumPhotos
import mega.privacy.android.domain.usecase.GetUserAlbum
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AlbumGetLinkViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getUserAlbumUseCase: GetUserAlbum,
    private val getAlbumPhotosUseCase: GetAlbumPhotos,
    private val downloadThumbnailUseCase: DownloadThumbnail,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val state = MutableStateFlow(value = AlbumGetLinkState())
    val stateFlow = state.asStateFlow()

    fun initialize() {
        fetchAlbum()
        fetchLink()

        state.update {
            it.copy(isInitialized = true)
        }
    }

    private fun fetchAlbum() =
        savedStateHandle.getStateFlow<Long?>(ALBUM_ID, null)
            .filterNotNull()
            .map(::getAlbumSummary)
            .filterNotNull()
            .onEach(::updateAlbumSummary)
            .catch { exception -> Timber.e(exception) }
            .launchIn(viewModelScope)

    private suspend fun getAlbumSummary(id: Long): AlbumSummary? {
        val album = getUserAlbumUseCase(albumId = AlbumId(id)).firstOrNull() ?: return null
        val photos = getAlbumPhotosUseCase(albumId = album.id).firstOrNull() ?: return null

        return AlbumSummary(
            album = Album.UserAlbum(
                id = album.id,
                title = album.title,
                cover = album.cover ?: withContext(defaultDispatcher) {
                    photos.maxByOrNull { it.modificationTime }
                },
                modificationTime = album.modificationTime,
            ),
            numPhotos = photos.size,
        )
    }

    private fun updateAlbumSummary(albumSummary: AlbumSummary) {
        state.update {
            it.copy(albumSummary = albumSummary)
        }
    }

    private fun fetchLink() = viewModelScope.launch {
        delay(5000L)
        state.update {
            it.copy(link = "https://mega.nz/folder/yhMQkSaB#ndFn_kn1WY6l74Lzdm4VJQ")
        }
    }

    fun downloadImage(
        photo: Photo,
        callback: (Boolean) -> Unit,
    ) = viewModelScope.launch(ioDispatcher) {
        val thumbnailFilePath = photo.thumbnailFilePath ?: return@launch

        if (File(thumbnailFilePath).exists()) callback(true)
        else downloadThumbnailUseCase(nodeId = photo.id, callback)
    }

    fun toggleSeparateKeyEnabled(checked: Boolean) {
        state.update {
            it.copy(isSeparateKeyEnabled = !it.isSeparateKeyEnabled)
        }
    }
}
