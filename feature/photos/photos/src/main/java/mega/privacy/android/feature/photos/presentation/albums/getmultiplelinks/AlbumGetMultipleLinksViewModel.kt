package mega.privacy.android.feature.photos.presentation.albums.getmultiplelinks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
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
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.GetAlbumPhotos
import mega.privacy.android.domain.usecase.GetUserAlbum
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.domain.usecase.SetShowCopyrightUseCase
import mega.privacy.android.domain.usecase.ShouldShowCopyrightUseCase
import mega.privacy.android.domain.usecase.photos.AlbumHasSensitiveContentUseCase
import mega.privacy.android.domain.usecase.photos.ExportAlbumsUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.DownloadThumbnailUseCase
import mega.privacy.android.feature.photos.presentation.albums.getlink.AlbumSummary
import timber.log.Timber
import java.io.File

/**
 * ViewModel for GetMultipleLinks
 */
@HiltViewModel(assistedFactory = AlbumGetMultipleLinksViewModel.Factory::class)
class AlbumGetMultipleLinksViewModel @AssistedInject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getUserAlbumUseCase: GetUserAlbum,
    private val getAlbumPhotosUseCase: GetAlbumPhotos,
    private val downloadThumbnailUseCase: DownloadThumbnailUseCase,
    private val exportAlbumsUseCase: ExportAlbumsUseCase,
    private val shouldShowCopyrightUseCase: ShouldShowCopyrightUseCase,
    private val setShowCopyrightUseCase: SetShowCopyrightUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val monitorThemeModeUseCase: MonitorThemeModeUseCase,
    private val albumHasSensitiveContentUseCase: AlbumHasSensitiveContentUseCase,
    @Assisted private val albumIds: LongArray?,
) : ViewModel() {
    private val state = MutableStateFlow(value = AlbumGetMultipleLinksState())
    val stateFlow = state.asStateFlow()

    init {
        monitorThemeMode()
        initialize()
    }

    private fun monitorThemeMode() {
        monitorThemeModeUseCase()
            .catch { Timber.e(it) }
            .onEach { themeMode ->
                state.update {
                    it.copy(themeMode = themeMode)
                }
            }
            .launchIn(viewModelScope)
    }

    fun initialize() {
        viewModelScope.launch {
            val showCopyright = shouldShowCopyrightUseCase()
            val hasSensitiveElement = savedStateHandle
                .getStateFlow(ALBUM_ID, albumIds)
                .value
                ?.map { AlbumId(it) }
                ?.any {
                    runCatching {
                        albumHasSensitiveContentUseCase(it)
                    }.getOrDefault(false)
                }
                ?: false

            if (!showCopyright && !hasSensitiveElement) {
                fetchAlbums()
                fetchLinks()
            }

            state.update {
                it.copy(
                    isInitialized = true,
                    showCopyright = showCopyright,
                    showSharingSensitiveWarning = hasSensitiveElement,
                )
            }
        }
    }

    fun fetchAlbums() =
        savedStateHandle.getStateFlow(ALBUM_ID, albumIds)
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

    fun fetchLinks() =
        savedStateHandle.getStateFlow(ALBUM_ID, albumIds)
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
        else {
            runCatching { downloadThumbnailUseCase(photo.id) }
                .onSuccess { callback(true) }
                .onFailure { callback(false) }
        }
    }

    fun hideCopyright() {
        state.update {
            it.copy(showCopyright = false)
        }
    }

    fun hideSharingSensitiveWarning() {
        state.update {
            it.copy(showSharingSensitiveWarning = false)
        }
    }

    /**
     * Updates the flag to show or not show copyright in DB.
     *
     * @param show True to show copyright, false to hide it.
     */
    fun updateShowCopyRight(show: Boolean) {
        viewModelScope.launch {
            runCatching {
                setShowCopyrightUseCase(show)
            }
        }
    }

    /**
     * Marks that the user has agreed to copyright terms.
     */
    fun agreeCopyrightTerms() {
        state.update {
            it.copy(copyrightAgreed = true)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(albumIds: LongArray?): AlbumGetMultipleLinksViewModel
    }

    companion object {
        /**
         * Navigation argument key for the album IDs
         */
        const val ALBUM_ID: String = "album_id"

        /**
         * Navigation argument key for indicating if the album has sensitive elements
         */
        const val HAS_SENSITIVE_ELEMENT: String = "has_sensitive_element"
    }
}
