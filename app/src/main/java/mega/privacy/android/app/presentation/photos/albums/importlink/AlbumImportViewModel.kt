package mega.privacy.android.app.presentation.photos.albums.importlink

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
import mega.privacy.android.app.presentation.photos.util.LegacyPublicAlbumPhotoNodeProvider
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.Album.UserAlbum
import mega.privacy.android.domain.entity.photos.AlbumLink
import mega.privacy.android.domain.entity.photos.AlbumPhotoIds
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.usecase.GetUserAlbums
import mega.privacy.android.domain.usecase.HasCredentials
import mega.privacy.android.domain.usecase.photos.DownloadPublicAlbumPhotoPreviewUseCase
import mega.privacy.android.domain.usecase.photos.DownloadPublicAlbumPhotoThumbnailUseCase
import mega.privacy.android.domain.usecase.photos.GetProscribedAlbumNamesUseCase
import mega.privacy.android.domain.usecase.photos.GetPublicAlbumPhotoUseCase
import mega.privacy.android.domain.usecase.photos.GetPublicAlbumUseCase
import mega.privacy.android.domain.usecase.photos.ImportPublicAlbumUseCase
import timber.log.Timber
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class AlbumImportViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val hasCredentialsUseCase: HasCredentials,
    private val getUserAlbums: GetUserAlbums,
    private val getPublicAlbumUseCase: GetPublicAlbumUseCase,
    private val getPublicAlbumPhotoUseCase: GetPublicAlbumPhotoUseCase,
    private val legacyPublicAlbumPhotoNodeProvider: LegacyPublicAlbumPhotoNodeProvider,
    private val downloadPublicAlbumPhotoPreviewUseCase: DownloadPublicAlbumPhotoPreviewUseCase,
    private val downloadPublicAlbumPhotoThumbnailUseCase: DownloadPublicAlbumPhotoThumbnailUseCase,
    private val getProscribedAlbumNamesUseCase: GetProscribedAlbumNamesUseCase,
    private val getStringFromStringResMapper: GetStringFromStringResMapper,
    private val importPublicAlbumUseCase: ImportPublicAlbumUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val state = MutableStateFlow(value = AlbumImportState())
    val stateFlow = state.asStateFlow()

    private val albumLink: String?
        get() = savedStateHandle[ALBUM_LINK]

    @Volatile
    @VisibleForTesting
    var localAlbumNames: Set<String> = setOf()

    private var albumNameToImport: String = ""

    private var photosToImport: Collection<Photo> = listOf()

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
            handlePublicAlbum(link, albumPhotos)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun handlePublicAlbum(link: String, albumPhotos: AlbumPhotoIds) {
        val (album, albumPhotoIds) = albumPhotos

        val result = coroutineScope {
            awaitAll(
                async { getPublicAlbumPhotoUseCase(albumPhotoIds) },
                async { legacyPublicAlbumPhotoNodeProvider.loadNodeCache(albumPhotoIds) },
            )
        }

        updateAlbumPhotos(link, album, result[0] as List<Photo>)
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
        if (isPreview) {
            downloadPublicAlbumPhotoPreviewUseCase(photo, callback)
        } else {
            downloadPublicAlbumPhotoThumbnailUseCase(photo, callback)
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

        albumNameToImport = albumName

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

    fun clearRenameAlbumValid() {
        state.update {
            it.copy(isRenameAlbumValid = false)
        }
    }

    fun validateImportConstraint(
        album: UserAlbum?,
        photos: Collection<Photo>,
    ) = viewModelScope.launch {
        albumNameToImport = album?.title.orEmpty()
        photosToImport = photos

        val checkAvailableStorage = { false }

        val checkAlbumNameConflict = {
            val isInvalid = album?.title in localAlbumNames

            state.update {
                it.copy(showRenameAlbumDialog = isInvalid)
            }
            isInvalid
        }

        val constraints = listOf(
            { checkAvailableStorage() },
            { checkAlbumNameConflict() },
        )

        for (constraint in constraints) {
            if (constraint()) return@launch
        }

        state.update {
            it.copy(isImportConstraintValid = true)
        }
    }

    fun clearImportConstraintValid() {
        state.update {
            it.copy(isImportConstraintValid = false)
        }
    }

    fun importAlbum(targetParentFolderNodeId: NodeId) = viewModelScope.launch {
        state.update {
            it.copy(showImportAlbumDialog = true)
        }

        val photoIds = withContext(defaultDispatcher) {
            photosToImport.map { NodeId(it.id) }
        }

        runCatching {
            importPublicAlbumUseCase(
                albumName = albumNameToImport,
                photoIds = photoIds,
                targetParentFolderNodeId = targetParentFolderNodeId,
            )
        }.onFailure {
            state.update {
                it.copy(
                    showImportAlbumDialog = false,
                    importAlbumMessage = getStringFromStringResMapper(
                        stringId = R.string.album_import_error_message,
                        albumNameToImport,
                    ),
                )
            }
        }.onSuccess {
            state.update {
                it.copy(
                    showImportAlbumDialog = false,
                    importAlbumMessage = getStringFromStringResMapper(
                        stringId = R.string.album_import_success_message,
                        albumNameToImport,
                    ),
                )
            }
        }
    }

    fun clearImportAlbumMessage() {
        state.update {
            it.copy(importAlbumMessage = null)
        }
    }

    fun mapPhotosToNodes(photos: Collection<Photo>) = photos.mapNotNull { photo ->
        legacyPublicAlbumPhotoNodeProvider.getPublicNode(photo.id)
    }
}
