package mega.privacy.android.app.presentation.photos.albums.getlink

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
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
import mega.privacy.android.domain.usecase.thumbnailpreview.DownloadThumbnailUseCase
import mega.privacy.android.domain.usecase.GetAlbumPhotos
import mega.privacy.android.domain.usecase.GetUserAlbum
import mega.privacy.android.domain.usecase.ShouldShowCopyrightUseCase
import mega.privacy.android.domain.usecase.photos.ExportAlbumsUseCase
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AlbumGetLinkViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getUserAlbumUseCase: GetUserAlbum,
    private val getAlbumPhotosUseCase: GetAlbumPhotos,
    private val downloadThumbnailUseCase: DownloadThumbnailUseCase,
    private val exportAlbumsUseCase: ExportAlbumsUseCase,
    private val shouldShowCopyrightUseCase: ShouldShowCopyrightUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val state = MutableStateFlow(value = AlbumGetLinkState())
    val stateFlow = state.asStateFlow()

    fun initialize() = viewModelScope.launch {
        val showCopyright = shouldShowCopyrightUseCase()
        if (!showCopyright) {
            fetchAlbum()
        }

        state.update {
            it.copy(
                isInitialized = true,
                showCopyright = showCopyright,
            )
        }
    }

    fun hideCopyright() {
        state.update {
            it.copy(showCopyright = false)
        }
    }

    fun fetchAlbum() =
        savedStateHandle.getStateFlow<Long?>(ALBUM_ID, null)
            .filterNotNull()
            .map(::getAlbumSummary)
            .onEach(::updateLink)
            .catch { exception -> Timber.e(exception) }
            .launchIn(viewModelScope)

    private suspend fun getAlbumSummary(id: Long): AlbumSummary? {
        val album = getUserAlbumUseCase(albumId = AlbumId(id)).firstOrNull() ?: return null
        val photos = getAlbumPhotosUseCase(albumId = album.id).firstOrNull().orEmpty()

        return AlbumSummary(
            album = Album.UserAlbum(
                id = album.id,
                title = album.title,
                cover = album.cover ?: withContext(defaultDispatcher) {
                    photos.maxByOrNull { it.modificationTime }
                },
                creationTime = album.creationTime,
                modificationTime = album.modificationTime,
                isExported = album.isExported,
            ),
            numPhotos = photos.size,
        )
    }

    private fun updateLink(albumSummary: AlbumSummary?) = viewModelScope.launch {
        val albumIdLinks = exportAlbumsUseCase(albumIds = listOfNotNull(albumSummary?.album?.id))
        val albumIdLink = albumIdLinks.firstOrNull()
        val exitScreen = albumSummary == null || albumIdLink?.first != albumSummary.album.id

        state.update {
            it.copy(
                albumSummary = albumSummary,
                link = albumIdLink?.second?.link.orEmpty(),
                exitScreen = exitScreen,
            )
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
