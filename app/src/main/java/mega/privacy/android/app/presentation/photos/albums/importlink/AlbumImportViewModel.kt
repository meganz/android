package mega.privacy.android.app.presentation.photos.albums.importlink

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.StringsConstants.INVALID_CHARACTERS
import mega.privacy.android.app.presentation.mapper.GetStringFromStringResMapper
import mega.privacy.android.app.presentation.photos.albums.AlbumScreenWrapperActivity.Companion.ALBUM_LINK
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.Album.UserAlbum
import mega.privacy.android.domain.entity.photos.AlbumLink
import mega.privacy.android.domain.entity.photos.AlbumPhotoIds
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.usecase.GetUserAlbums
import mega.privacy.android.domain.usecase.HasCredentialsUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.filelink.GetPublicNodeFromSerializedDataUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.photos.DownloadPublicAlbumPhotoPreviewUseCase
import mega.privacy.android.domain.usecase.photos.DownloadPublicAlbumPhotoThumbnailUseCase
import mega.privacy.android.domain.usecase.photos.GetProscribedAlbumNamesUseCase
import mega.privacy.android.domain.usecase.photos.GetPublicAlbumNodesDataUseCase
import mega.privacy.android.domain.usecase.photos.GetPublicAlbumPhotoUseCase
import mega.privacy.android.domain.usecase.photos.GetPublicAlbumUseCase
import mega.privacy.android.domain.usecase.photos.ImportPublicAlbumUseCase
import mega.privacy.android.domain.usecase.photos.IsAlbumLinkValidUseCase
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
internal class AlbumImportViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val hasCredentialsUseCase: HasCredentialsUseCase,
    private val getUserAlbums: GetUserAlbums,
    private val getPublicAlbumUseCase: GetPublicAlbumUseCase,
    private val getPublicAlbumPhotoUseCase: GetPublicAlbumPhotoUseCase,
    private val downloadPublicAlbumPhotoPreviewUseCase: DownloadPublicAlbumPhotoPreviewUseCase,
    private val downloadPublicAlbumPhotoThumbnailUseCase: DownloadPublicAlbumPhotoThumbnailUseCase,
    private val getProscribedAlbumNamesUseCase: GetProscribedAlbumNamesUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val getStringFromStringResMapper: GetStringFromStringResMapper,
    private val importPublicAlbumUseCase: ImportPublicAlbumUseCase,
    private val isAlbumLinkValidUseCase: IsAlbumLinkValidUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val getPublicNodeFromSerializedDataUseCase: GetPublicNodeFromSerializedDataUseCase,
    private val getPublicAlbumNodesDataUseCase: GetPublicAlbumNodesDataUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val state = MutableStateFlow(value = AlbumImportState())
    val stateFlow = state.asStateFlow()

    private val albumLink: String?
        get() = savedStateHandle[ALBUM_LINK]

    private var albumNameToImport: String = ""

    private var photosToImport: Collection<Photo> = listOf()

    private var importAlbumJob: Job? = null

    @Volatile
    @VisibleForTesting
    var isNetworkConnected: Boolean = false

    @Volatile
    @VisibleForTesting
    var localAlbumNames: Set<String> = setOf()

    @Volatile
    @VisibleForTesting
    var availableStorage: Long = 0

    init {
        initialize()
    }

    fun initialize() = viewModelScope.launch {
        monitorNetworkConnection()
        handleSharedAlbumLink(link = albumLink)
        validateLink(link = albumLink)

        val isLogin = hasCredentialsUseCase()
        if (isLogin) {
            loadUserAlbums()
            monitorAccountDetail()
        }

        state.update {
            it.copy(
                isLogin = isLogin,
                isLocalAlbumsLoaded = !isLogin,
                isAvailableStorageCollected = it.isAvailableStorageCollected || !isLogin,
            )
        }
    }

    private fun monitorNetworkConnection() {
        var isFirstEmission = true
        monitorConnectivityUseCase()
            .debounce { isConnected ->
                if (isConnected || isFirstEmission) {
                    isFirstEmission = false
                    0.seconds
                } else {
                    1.seconds
                }
            }
            .onEach(::handleNetworkConnection)
            .launchIn(viewModelScope)
    }

    private fun handleNetworkConnection(isConnected: Boolean) {
        isNetworkConnected = isConnected
        state.update {
            it.copy(isNetworkConnected = isConnected)
        }

        if (isNetworkConnected) return
        cancelImportAlbum()
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

    private suspend fun handlePublicAlbum(link: String, albumPhotos: AlbumPhotoIds) {
        val (album, albumPhotoIds) = albumPhotos

        runCatching {
            getPublicAlbumPhotoUseCase(albumPhotoIds)
        }.onFailure {
            state.update {
                it.copy(showErrorAccessDialog = true)
            }
        }.onSuccess { result ->
            updateAlbumPhotos(link, album, result)
        }
    }

    private suspend fun updateAlbumPhotos(
        link: String,
        album: UserAlbum,
        photos: List<Photo>,
    ) {
        val sortedPhotos = withContext(defaultDispatcher) {
            photos.sortedByDescending { it.modificationTime }
        }

        val openedPhoto = state.value.folderSubHandle?.let { subHandle ->
            sortedPhotos.firstOrNull { it.base64Id == subHandle }
        }

        state.update {
            it.copy(
                link = link,
                album = album,
                photos = sortedPhotos,
                openFileNodeEvent = if (openedPhoto != null) {
                    triggered(openedPhoto)
                } else {
                    consumed()
                },
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

        val checkAvailableStorage = {
            val isInvalid = photos.sumOf { it.size } > availableStorage

            state.update {
                it.copy(showStorageExceededDialog = isInvalid)
            }
            isInvalid
        }

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

    fun importAlbum(targetParentFolderNodeId: NodeId) {
        if (!isNetworkConnected) {
            state.update {
                it.copy(
                    importAlbumMessage = getStringFromStringResMapper(
                        stringId = R.string.error_server_connection_problem,
                    ),
                )
            }
            return
        }

        importAlbumJob?.cancel()
        importAlbumJob = viewModelScope.launch {
            state.update {
                it.copy(showImportAlbumDialog = true)
            }

            runCatching {
                if (!isAlbumLinkValidUseCase(albumLink = AlbumLink(state.value.link.orEmpty()))) {
                    state.update {
                        it.copy(
                            showErrorAccessDialog = true,
                            showImportAlbumDialog = false,
                            isBackToHome = true,
                        )
                    }
                    return@launch
                }

                val photoIds = withContext(defaultDispatcher) {
                    photosToImport.map { NodeId(it.id) }
                }

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
    }

    private fun cancelImportAlbum() {
        if (!state.value.showImportAlbumDialog) return
        importAlbumJob?.cancel()

        state.update {
            it.copy(
                showImportAlbumDialog = false,
                importAlbumMessage = getStringFromStringResMapper(
                    stringId = R.string.album_import_error_message,
                    albumNameToImport,
                ),
            )
        }
    }

    fun clearImportAlbumMessage() {
        state.update {
            it.copy(importAlbumMessage = null)
        }
    }

    private fun monitorAccountDetail() = monitorAccountDetailUseCase()
        .onEach(::handleAccountDetail)
        .launchIn(viewModelScope)

    private fun handleAccountDetail(accountDetail: AccountDetail) {
        availableStorage = accountDetail.storageDetail?.availableSpace ?: 0L

        state.update {
            it.copy(isAvailableStorageCollected = true)
        }
    }

    fun closeStorageExceededDialog() {
        state.update {
            it.copy(showStorageExceededDialog = false)
        }
    }

    fun startDownload() {
        viewModelScope.launch {
            val photos = with(state.value) { selectedPhotos.ifEmpty { photos } }
            val data = getPublicAlbumNodesDataUseCase()
            val nodes = photos
                .mapNotNull { data[NodeId(it.id)] }
                .mapNotNull { getPublicNodeFromSerializedDataUseCase(it) }
            updateDownloadEvent(
                TransferTriggerEvent.StartDownloadNode(
                    nodes = nodes,
                    withStartMessage = true,
                )
            )
            clearSelection()
        }
    }

    fun consumeDownloadEvent() = updateDownloadEvent(null)

    private fun updateDownloadEvent(event: TransferTriggerEvent?) =
        state.update {
            it.copy(downloadEvent = event?.let { triggered(event) } ?: consumed())
        }

    /**
     * Extract sub-handle from album link
     * Expected URL format: https://mega.nz/collection/handle#key!subHandle123
     *
     * @param link The album link to process, defaults to albumLink property
     */
    private fun handleSharedAlbumLink(link: String? = albumLink) {
        if (link.isNullOrBlank()) {
            Timber.e("Album link is null or empty")
            return
        }

        Timber.d("Processing album URL: $link")

        val subHandle = link.substringAfterLast("!", "")
            .takeIf { it.isNotBlank() }
            ?: run {
                Timber.e("No sub-handle found in album URL")
                return
            }

        Timber.d("Extracted sub-handle: $subHandle")
        state.update { it.copy(folderSubHandle = subHandle) }
    }

    /**
     * Open file using Photo
     * This method triggers the file opening event for the Compose view to handle
     *
     * @param photo The Photo object representing the file to be opened
     */
    fun openFile(photo: Photo) {
        state.update {
            it.copy(openFileNodeEvent = triggered(photo))
        }
    }

    /**
     * Reset and notify that openFileNodeEvent is consumed
     */
    fun resetOpenFileNodeEvent() {
        state.update {
            it.copy(openFileNodeEvent = consumed())
        }
    }
}
