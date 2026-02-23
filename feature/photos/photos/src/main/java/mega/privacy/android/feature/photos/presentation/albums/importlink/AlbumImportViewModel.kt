package mega.privacy.android.feature.photos.presentation.albums.importlink

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.Album.UserAlbum
import mega.privacy.android.domain.entity.photos.AlbumLink
import mega.privacy.android.domain.entity.photos.AlbumPhotoIds
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.usecase.GetUserAlbums
import mega.privacy.android.domain.usecase.HasCredentialsUseCase
import mega.privacy.android.domain.usecase.account.GetCurrentStorageStateUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.filelink.GetPublicNodeFromSerializedDataUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.CheckForValidNameUseCase.Companion.isInvalidDotName
import mega.privacy.android.domain.usecase.node.CheckForValidNameUseCase.Companion.isInvalidDoubleDotName
import mega.privacy.android.domain.usecase.photos.GetProscribedAlbumNamesUseCase
import mega.privacy.android.domain.usecase.photos.GetPublicAlbumNodesDataUseCase
import mega.privacy.android.domain.usecase.photos.GetPublicAlbumPhotoUseCase
import mega.privacy.android.domain.usecase.photos.GetPublicAlbumUseCase
import mega.privacy.android.domain.usecase.photos.ImportPublicAlbumUseCase
import mega.privacy.android.domain.usecase.photos.IsAlbumLinkValidUseCase
import mega.privacy.android.feature.photos.mapper.PhotoUiStateMapper
import mega.privacy.android.feature.photos.model.PhotoUiState
import mega.privacy.android.shared.resources.R as sharedR
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

private const val INVALID_CHARACTERS = "\" * / : < > ? \\ |"

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel(assistedFactory = AlbumImportViewModel.Factory::class)
class AlbumImportViewModel @AssistedInject constructor(
    private val photoUiStateMapper: PhotoUiStateMapper,
    private val hasCredentialsUseCase: HasCredentialsUseCase,
    private val getUserAlbums: GetUserAlbums,
    private val getPublicAlbumUseCase: GetPublicAlbumUseCase,
    private val getPublicAlbumPhotoUseCase: GetPublicAlbumPhotoUseCase,
    private val getProscribedAlbumNamesUseCase: GetProscribedAlbumNamesUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val importPublicAlbumUseCase: ImportPublicAlbumUseCase,
    private val isAlbumLinkValidUseCase: IsAlbumLinkValidUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val getPublicNodeFromSerializedDataUseCase: GetPublicNodeFromSerializedDataUseCase,
    private val getPublicAlbumNodesDataUseCase: GetPublicAlbumNodesDataUseCase,
    private val getCurrentStorageStateUseCase: GetCurrentStorageStateUseCase,
    @ApplicationContext private val context: Context,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    @Assisted private val albumLink: String?,
) : ViewModel() {
    private val state = MutableStateFlow(value = AlbumImportState())
    val stateFlow = state.asStateFlow()

    private var albumNameToImport: String = ""

    private var photosToImport: Collection<PhotoUiState> = listOf()

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
        viewModelScope.launch {
            monitorNetworkConnection()
            handleSharedAlbumLink()
            validateLink()

            val isLogin = hasCredentialsUseCase()
            if (isLogin) {
                getStorageState()
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
    }

    private suspend fun getStorageState() {
        runCatching {
            getCurrentStorageStateUseCase()
        }.onSuccess { storageState ->
            state.update {
                it.copy(storageState = storageState)
            }
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

    private suspend fun validateLink() {
        if (albumLink == null) {
            state.update {
                it.copy(showErrorAccessDialog = true)
            }
        } else if (!albumLink.contains("#")) {
            state.update {
                it.copy(showInputDecryptionKeyDialog = true)
            }
        } else {
            fetchPublicAlbum(albumLink)
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
        val sortedPhotosUiState = withContext(defaultDispatcher) {
            photos.map { photoUiStateMapper(it) }
                .sortedByDescending { it.modificationTime }
        }

        val openedPhoto = state.value.folderSubHandle?.let { subHandle ->
            sortedPhotosUiState.firstOrNull { it.base64Id == subHandle }
        }

        state.update {
            it.copy(
                link = link,
                album = album,
                photos = sortedPhotosUiState,
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

    fun closeInputDecryptionKeyDialog() {
        state.update {
            it.copy(showInputDecryptionKeyDialog = false)
        }
    }

    fun decryptLink(key: String) = viewModelScope.launch {
        fetchPublicAlbum(link = "${albumLink}#$key")
    }

    fun selectPhoto(photo: PhotoUiState) {
        state.update {
            it.copy(selectedPhotos = it.selectedPhotos + photo)
        }
    }

    fun selectAllPhotos() {
        state.update {
            it.copy(selectedPhotos = it.photos.toSet())
        }
    }

    fun unselectPhoto(photo: PhotoUiState) {
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

    fun validateAlbumName(albumName: String) {
        viewModelScope.launch {
            val checkBlankName = {
                val isInvalid = albumName.isBlank()

                if (isInvalid) {
                    state.update {
                        it.copy(renameAlbumErrorMessage = context.getString(sharedR.string.album_import_rename_album_dialog_empty_name_error))
                    }
                }

                isInvalid
            }

            val checkDotName = {
                val isInvalid = albumName.isInvalidDotName()

                if (isInvalid) {
                    state.update {
                        it.copy(
                            renameAlbumErrorMessage = context.getString(
                                sharedR.string.general_invalid_dot_name_warning,
                            ),
                        )
                    }
                }
                isInvalid
            }

            val checkDoubleDotName = {
                val isInvalid = albumName.isInvalidDoubleDotName()

                if (isInvalid) {
                    state.update {
                        it.copy(
                            renameAlbumErrorMessage = context.getString(
                                sharedR.string.general_invalid_double_dot_name_warning,
                            ),
                        )
                    }
                }
                isInvalid
            }

            val checkInvalidChar = {
                val isInvalid = "[\\\\*/:<>?\"|]".toRegex().containsMatchIn(albumName)

                if (isInvalid) {
                    state.update {
                        it.copy(
                            renameAlbumErrorMessage = context.getString(
                                sharedR.string.general_invalid_characters_defined,
                                INVALID_CHARACTERS,
                            ),
                        )
                    }
                }
                isInvalid
            }

            val checkDuplicatedName = {
                val isInvalid = albumName in localAlbumNames

                if (isInvalid) {
                    state.update {
                        it.copy(
                            renameAlbumErrorMessage = context.getString(
                                sharedR.string.photos_create_album_duplicate_name_error,
                            ),
                        )
                    }
                }
                isInvalid
            }

            val checkProscribedName = suspend {
                val proscribedNames = getProscribedAlbumNamesUseCase()
                val isInvalid = albumName.lowercase() in proscribedNames.map { it.lowercase() }

                if (isInvalid) {
                    state.update {
                        it.copy(
                            renameAlbumErrorMessage = context.getString(
                                sharedR.string.photos_create_album_proscribed_name_error,
                            ),
                        )
                    }
                }
                isInvalid
            }

            val constraints = listOf(
                { checkBlankName() },
                { checkDotName() },
                { checkDoubleDotName() },
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
        photos: Collection<PhotoUiState>,
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
                    addToCloudDriveFinishedEvent = triggered,
                    importAlbumMessage = context.getString(sharedR.string.photos_network_error_message),
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
                            addToCloudDriveFinishedEvent = triggered
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
                        addToCloudDriveFinishedEvent = triggered,
                        showImportAlbumDialog = false,
                        importAlbumMessage = context.getString(
                            sharedR.string.album_import_snackbar_error_message,
                            albumNameToImport,
                        ),
                    )
                }
            }.onSuccess {
                state.update {
                    it.copy(
                        addToCloudDriveFinishedEvent = triggered,
                        showImportAlbumDialog = false,
                        importAlbumMessage = context.getString(
                            sharedR.string.album_import_snackbar_success_message,
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
                importAlbumMessage = context.getString(
                    sharedR.string.album_import_snackbar_error_message,
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
     * Expected URL format: [ALBUM_LINK_REGEXS]
     */
    private fun handleSharedAlbumLink() {
        if (albumLink.isNullOrBlank()) {
            Timber.e("Album link is null or empty")
            return
        }

        Timber.d("Processing album URL: $albumLink")

        val subHandle = albumLink.substringAfterLast("!", "")
            .takeIf { it.isNotBlank() }
            ?: run {
                Timber.e("No sub-handle found in album URL")
                return
            }

        Timber.d("Extracted sub-handle: $subHandle")
        state.update { it.copy(folderSubHandle = subHandle) }
    }

    /**
     * Open file using PhotoUiState
     * This method triggers the file opening event for the Compose view to handle
     *
     * @param photo The PhotoUiState representing the file to be opened
     */
    fun openFile(photo: PhotoUiState) {
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

    fun resetAlbumSelectionFinishedEvent() {
        state.update {
            it.copy(addToCloudDriveFinishedEvent = consumed)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(albumLink: String?): AlbumImportViewModel
    }

    companion object {
        const val ALBUM_LINK = "album_link"
    }
}
