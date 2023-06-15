package mega.privacy.android.app.presentation.photos.albums.importlink

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.StringsConstants.INVALID_CHARACTERS
import mega.privacy.android.app.presentation.mapper.GetStringFromStringResMapper
import mega.privacy.android.app.presentation.photos.albums.AlbumScreenWrapperActivity.Companion.ALBUM_LINK
import mega.privacy.android.app.presentation.photos.util.LegacyPublicAlbumPhotoProvider
import mega.privacy.android.domain.entity.photos.Album.UserAlbum
import mega.privacy.android.domain.entity.photos.AlbumLink
import mega.privacy.android.domain.entity.photos.AlbumPhotoId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.usecase.GetUserAlbums
import mega.privacy.android.domain.usecase.HasCredentials
import mega.privacy.android.domain.usecase.photos.GetProscribedAlbumNamesUseCase
import mega.privacy.android.domain.usecase.photos.GetPublicAlbumUseCase
import timber.log.Timber
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class AlbumImportViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val hasCredentialsUseCase: HasCredentials,
    private val getUserAlbums: GetUserAlbums,
    private val getPublicAlbumUseCase: GetPublicAlbumUseCase,
    private val legacyPublicAlbumPhotoProvider: LegacyPublicAlbumPhotoProvider,
    private val getProscribedAlbumNamesUseCase: GetProscribedAlbumNamesUseCase,
    private val getStringFromStringResMapper: GetStringFromStringResMapper,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val state = MutableStateFlow(value = AlbumImportState())
    val stateFlow = state.asStateFlow()

    private val albumLink: String?
        get() = savedStateHandle[ALBUM_LINK]

    @Volatile
    var localAlbumNames: Set<String> = setOf()
        private set

    fun initialize() = viewModelScope.launch {
        validateLink(link = albumLink)

        val isLogin = hasCredentialsUseCase()
        if (isLogin) loadUserAlbums()

        state.update {
            it.copy(
                isInitialized = true,
                isLogin = isLogin,
                isLocalAlbumsLoaded = !isLogin,
            )
        }
    }

    private suspend fun validateLink(link: String?) {
        if (link == null) {
            state.update {
                it.copy(showErrorAccessDialog = true)
            }
        } else if (!link.contains("#")) {
            state.update {
                it.copy(showInputDecryptionKeyDialog = true)
            }
        } else {
            fetchPublicAlbum(link)
        }
    }

    private suspend fun fetchPublicAlbum(link: String) {
        runCatching {
            getPublicAlbumUseCase(albumLink = AlbumLink(link))
        }.onFailure {
            state.update {
                it.copy(showErrorAccessDialog = true)
            }
        }.onSuccess { albumPhotos ->
            val (album, photoIds) = albumPhotos
            val photos = fetchPublicPhotos(photoIds)
            updateAlbumPhotos(link, album, photos)
        }
    }

    private suspend fun fetchPublicPhotos(photosIds: List<AlbumPhotoId>): List<Photo> {
        return legacyPublicAlbumPhotoProvider.getPublicPhotos(photosIds)
    }

    private suspend fun updateAlbumPhotos(
        link: String,
        album: UserAlbum,
        photos: List<Photo>,
    ) {
        val sortedPhotos = withContext(defaultDispatcher) {
            photos.sortedByDescending { it.modificationTime }
        }

        state.update {
            it.copy(
                link = link,
                album = album,
                photos = sortedPhotos,
            )
        }
    }

    private fun loadUserAlbums() = getUserAlbums()
        .catch { exception -> Timber.e(exception) }
        .mapLatest(::handleUserAlbums)
        .launchIn(viewModelScope)

    private suspend fun handleUserAlbums(albums: List<UserAlbum>) = withContext(defaultDispatcher) {
        localAlbumNames = albums.map { it.title }.toSet()

        state.update {
            it.copy(isLocalAlbumsLoaded = true)
        }
    }

    fun downloadImage(
        isPreview: Boolean,
        photo: Photo,
        callback: (Boolean) -> Unit,
    ) = viewModelScope.launch {
        with(legacyPublicAlbumPhotoProvider) {
            if (isPreview) {
                downloadPublicPreview(photo, callback)
            } else {
                downloadPublicThumbnail(photo, callback)
            }
        }
    }

    fun closeInputDecryptionKeyDialog() {
        state.update {
            it.copy(showInputDecryptionKeyDialog = false)
        }
    }

    fun decryptLink(key: String) = viewModelScope.launch {
        fetchPublicAlbum(link = "$albumLink#$key")
    }

    fun selectPhoto(photo: Photo) {
        state.update {
            it.copy(selectedPhotos = it.selectedPhotos + photo)
        }
    }

    fun selectAllPhotos() {
        state.update {
            it.copy(selectedPhotos = it.photos.toSet())
        }
    }

    fun unselectPhoto(photo: Photo) {
        state.update {
            it.copy(selectedPhotos = it.selectedPhotos - photo)
        }
    }

    fun clearSelection() {
        state.update {
            it.copy(selectedPhotos = setOf())
        }
    }

    fun closeRenameAlbumDialog() {
        state.update {
            it.copy(
                showRenameAlbumDialog = false,
                renameAlbumErrorMessage = null,
            )
        }
    }

    fun validateAlbumName(albumName: String) = viewModelScope.launch {
        val checkBlankName = {
            val isInvalid = albumName.isBlank()

            state.update {
                it.copy(
                    renameAlbumErrorMessage = getStringFromStringResMapper(
                        stringId = R.string.album_import_enter_album_name,
                    ).takeIf { isInvalid },
                )
            }
            isInvalid
        }

        val checkInvalidChar = {
            val isInvalid = "[\\\\*/:<>?\"|]".toRegex().containsMatchIn(albumName)

            state.update {
                it.copy(
                    renameAlbumErrorMessage = getStringFromStringResMapper(
                        stringId = R.string.invalid_characters_defined,
                        INVALID_CHARACTERS,
                    ).takeIf { isInvalid },
                )
            }
            isInvalid
        }

        val checkDuplicatedName = {
            val isInvalid = albumName in localAlbumNames

            state.update {
                it.copy(
                    renameAlbumErrorMessage = getStringFromStringResMapper(
                        stringId = R.string.photos_create_album_error_message_duplicate,
                    ).takeIf { isInvalid },
                )
            }
            isInvalid
        }

        val checkProscribedName = suspend {
            val proscribedNames = getProscribedAlbumNamesUseCase()
            val isInvalid = albumName.lowercase() in proscribedNames.map { it.lowercase() }

            state.update {
                it.copy(
                    renameAlbumErrorMessage = getStringFromStringResMapper(
                        stringId = R.string.photos_create_album_error_message_systems_album,
                    ).takeIf { isInvalid },
                )
            }
            isInvalid
        }

        val constraints = listOf(
            { checkBlankName() },
            { checkInvalidChar() },
            { checkDuplicatedName() },
            suspend { checkProscribedName() },
        )

        for (constraint in constraints) {
            if (constraint()) return@launch
        }

        state.update {
            it.copy(
                showRenameAlbumDialog = false,
                isRenameAlbumValid = true,
                renameAlbumErrorMessage = null,
            )
        }
    }

    fun clearRenameAlbumErrorMessage() {
        state.update {
            it.copy(renameAlbumErrorMessage = null)
        }
    }

    fun mapPhotosToNodes(photos: Collection<Photo>) = photos.mapNotNull { photo ->
        legacyPublicAlbumPhotoProvider.getPublicNode(photo.id)
    }
}
