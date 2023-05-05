package mega.privacy.android.app.presentation.photos.albums.getmultiplelinks

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
import mega.privacy.android.app.presentation.photos.albums.getlink.AlbumSummary
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.AlbumLink
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.DownloadThumbnail
import mega.privacy.android.domain.usecase.GetAlbumPhotos
import mega.privacy.android.domain.usecase.GetUserAlbum
import mega.privacy.android.domain.usecase.photos.ExportAlbumsUseCase
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for GetMultipleLinks
 */
@HiltViewModel
class AlbumGetMultipleLinksViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getUserAlbumUseCase: GetUserAlbum,
    private val getAlbumPhotosUseCase: GetAlbumPhotos,
    private val downloadThumbnailUseCase: DownloadThumbnail,
    private val exportAlbumsUseCase: ExportAlbumsUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val state = MutableStateFlow(value = AlbumGetMultipleLinksState())
    val stateFlow = state.asStateFlow()

    init {
        fetchAlbums()
        fetchLinks()

        state.update {
            it.copy(isInitialized = true)
        }
    }

    private fun fetchAlbums() =
        savedStateHandle.getStateFlow<LongArray?>(ALBUM_ID, null)
            .filterNotNull()
            .map(::getAlbumsSummaries)
            .filterNotNull()
            .onEach(::updateAlbumSummary)
            .catch { exception -> Timber.e(exception) }
            .launchIn(viewModelScope)

    private suspend fun getAlbumsSummaries(ids: LongArray): Map<AlbumId, AlbumSummary>? =
        ids.associate { albumId ->
            val album = getUserAlbumUseCase(albumId = AlbumId(albumId)).firstOrNull() ?: return null
            val photos = getAlbumPhotosUseCase(albumId = album.id).firstOrNull() ?: return null

            AlbumId(albumId) to AlbumSummary(
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

    private fun updateAlbumSummary(albumSummaries: Map<AlbumId, AlbumSummary>) {
        state.update {
            it.copy(albumsSummaries = albumSummaries)
        }
    }

    private fun fetchLinks() =
        savedStateHandle.getStateFlow<LongArray?>(ALBUM_ID, null)
            .filterNotNull()
            .map(::getAlbumLinks)
            .catch { exception -> Timber.e(exception) }
            .launchIn(viewModelScope)

    private suspend fun getAlbumLinks(ids: LongArray) = withContext(defaultDispatcher) {
        val links = exportAlbumsUseCase(ids.toList().map { AlbumId(it) }).associate {
            it.first to it.second
        }
        state.update {
            it.copy(
                albumLinks = links,
                albumLinksList = links.values.map { albumLink -> albumLink.link }.toList()

            )
        }
    }

    internal fun downloadImage(
        photo: Photo,
        callback: (Boolean) -> Unit,
    ) = viewModelScope.launch(ioDispatcher) {
        val thumbnailFilePath = photo.thumbnailFilePath ?: return@launch

        if (File(thumbnailFilePath).exists()) callback(true)
        else downloadThumbnailUseCase(nodeId = photo.id, callback)
    }
}
