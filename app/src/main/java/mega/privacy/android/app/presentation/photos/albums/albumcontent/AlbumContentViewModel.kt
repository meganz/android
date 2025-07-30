package mega.privacy.android.app.presentation.photos.albums.albumcontent

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.domain.usecase.GetNodeListByIds
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.photos.albums.model.mapper.UIAlbumMapper
import mega.privacy.android.app.presentation.photos.model.FilterMediaType
import mega.privacy.android.app.presentation.photos.model.Sort
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.Album.FavouriteAlbum
import mega.privacy.android.domain.entity.photos.Album.GifAlbum
import mega.privacy.android.domain.entity.photos.Album.RawAlbum
import mega.privacy.android.domain.entity.photos.Album.UserAlbum
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.AlbumPhotoId
import mega.privacy.android.domain.entity.photos.AlbumPhotosAddingProgress
import mega.privacy.android.domain.entity.photos.AlbumPhotosRemovingProgress
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.usecase.GetAlbumPhotosUseCase
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.GetDefaultAlbumPhotos
import mega.privacy.android.domain.usecase.GetUserAlbum
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.ObserveAlbumPhotosAddingProgress
import mega.privacy.android.domain.usecase.ObserveAlbumPhotosRemovingProgress
import mega.privacy.android.domain.usecase.UpdateAlbumPhotosAddingProgressCompleted
import mega.privacy.android.domain.usecase.UpdateAlbumPhotosRemovingProgressCompleted
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.favourites.RemoveFavouritesUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.photos.DisableExportAlbumsUseCase
import mega.privacy.android.domain.usecase.photos.GetDefaultAlbumsMapUseCase
import mega.privacy.android.domain.usecase.photos.GetProscribedAlbumNamesUseCase
import mega.privacy.android.domain.usecase.photos.RemovePhotosFromAlbumUseCase
import mega.privacy.android.domain.usecase.photos.UpdateAlbumNameUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.mobile.analytics.event.PhotoItemSelected
import mega.privacy.mobile.analytics.event.PhotoItemSelectedEvent
import nz.mega.sdk.MegaNode
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class AlbumContentViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getDefaultAlbumPhotos: GetDefaultAlbumPhotos,
    private val getDefaultAlbumsMapUseCase: GetDefaultAlbumsMapUseCase,
    private val getUserAlbum: GetUserAlbum,
    private val getAlbumPhotosUseCase: GetAlbumPhotosUseCase,
    private val uiAlbumMapper: UIAlbumMapper,
    private val observeAlbumPhotosAddingProgress: ObserveAlbumPhotosAddingProgress,
    private val updateAlbumPhotosAddingProgressCompleted: UpdateAlbumPhotosAddingProgressCompleted,
    private val observeAlbumPhotosRemovingProgress: ObserveAlbumPhotosRemovingProgress,
    private val updateAlbumPhotosRemovingProgressCompleted: UpdateAlbumPhotosRemovingProgressCompleted,
    private val disableExportAlbumsUseCase: DisableExportAlbumsUseCase,
    private val removeFavouritesUseCase: RemoveFavouritesUseCase,
    private val removePhotosFromAlbumUseCase: RemovePhotosFromAlbumUseCase,
    private val getNodeListByIds: GetNodeListByIds,
    private val getProscribedAlbumNamesUseCase: GetProscribedAlbumNamesUseCase,
    private val updateAlbumNameUseCase: UpdateAlbumNameUseCase,
    private val updateNodeSensitiveUseCase: UpdateNodeSensitiveUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val isHiddenNodesOnboardedUseCase: IsHiddenNodesOnboardedUseCase,
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(AlbumContentState())
    val state = _state.asStateFlow()

    private var observeAlbumPhotosJob: Job? = null

    var sourcePhotos: List<Photo>? = null
        private set

    private var showHiddenItems: Boolean? = null

    private val albumType: String?
        get() = savedStateHandle["type"]

    private val albumId: AlbumId?
        get() = savedStateHandle.get<Long?>("id")?.let { AlbumId(it) }

    private val photosFetchers: Map<String, () -> Unit> = mapOf(
        "favourite" to { fetchSystemPhotos(systemAlbum = FavouriteAlbum) },
        "gif" to { fetchSystemPhotos(systemAlbum = GifAlbum) },
        "raw" to { fetchSystemPhotos(systemAlbum = RawAlbum) },
        "custom" to { fetchAlbumPhotos() },
    )

    init {
        fetchPhotos()
        fetchIsHiddenNodesOnboarded()

        viewModelScope.launch {
            if (isHiddenNodesActive()) {
                monitorShowHiddenItems()
                monitorAccountDetail()
            }
        }
    }

    private suspend fun isHiddenNodesActive(): Boolean {
        val result = runCatching {
            getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)
        }
        return result.getOrNull() ?: false
    }

    private fun fetchPhotos() {
        val photosFetcher = photosFetchers[albumType] ?: return
        photosFetcher()
    }

    private fun fetchIsHiddenNodesOnboarded() = viewModelScope.launch {
        val isHiddenNodesOnboarded = isHiddenNodesOnboardedUseCase()
        _state.update {
            it.copy(isHiddenNodesOnboarded = isHiddenNodesOnboarded)
        }
    }

    private fun monitorShowHiddenItems() = monitorShowHiddenItemsUseCase()
        .onEach {
            showHiddenItems = it
            if (_state.value.isLoading) return@onEach

            val filteredPhotos = filterNonSensitivePhotos(photos = sourcePhotos.orEmpty())
            _state.update { state ->
                state.copy(photos = filteredPhotos)
            }

            updateSelection(filteredPhotos)
        }.launchIn(viewModelScope)

    private fun monitorAccountDetail() = monitorAccountDetailUseCase()
        .onEach { accountDetail ->
            val accountType = accountDetail.levelDetail?.accountType
            val businessStatus =
                if (accountType?.isBusinessAccount == true) {
                    getBusinessStatusUseCase()
                } else null

            _state.update {
                it.copy(
                    accountType = accountType,
                    isBusinessAccountExpired = businessStatus == BusinessAccountStatus.Expired,
                    hiddenNodeEnabled = true,
                )
            }
            if (_state.value.isLoading) return@onEach

            val filteredPhotos = filterNonSensitivePhotos(photos = sourcePhotos.orEmpty())
            _state.update { state ->
                state.copy(photos = filteredPhotos)
            }

            updateSelection(filteredPhotos)
        }.launchIn(viewModelScope)

    private fun updateSelection(photos: List<Photo>) {
        val selectedPhotos = _state.value.selectedPhotos.filter { selectedPhoto ->
            photos.any { it.id == selectedPhoto.id }
        }.toSet()

        _state.update {
            it.copy(selectedPhotos = selectedPhotos)
        }
    }

    private fun fetchSystemPhotos(systemAlbum: Album) {
        viewModelScope.launch {
            val filter = getDefaultAlbumsMapUseCase()[systemAlbum] ?: return@launch
            val isPaginationEnabled = runCatching {
                getFeatureFlagValueUseCase(AppFeatures.TimelinePhotosPagination)
            }.getOrDefault(false)

            runCatching {
                getDefaultAlbumPhotos(isPaginationEnabled, listOf(filter))
                    .onEach { sourcePhotos = it }
                    .map(::filterNonSensitivePhotos)
                    .onEach(::updateSelection)
                    .collectLatest { photos ->
                        val uiAlbum = uiAlbumMapper(
                            count = 0,
                            imageCount = 0,
                            videoCount = 0,
                            cover = null,
                            defaultCover = null,
                            album = systemAlbum,
                        )

                        _state.update {
                            it.copy(
                                isLoading = false,
                                uiAlbum = uiAlbum,
                                photos = photos,
                            )
                        }
                    }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    private fun fetchAlbumPhotos() {
        val albumId = albumId ?: return

        observeAlbum(albumId)
        observeAlbumPhotos(albumId, refresh = false)
        observePhotosAddingProgress(albumId)
        observePhotosRemovingProgress(albumId)
    }

    private fun observeAlbum(albumId: AlbumId) {
        viewModelScope.launch {
            runCatching {
                getUserAlbum(albumId)
                    .collectLatest { album ->
                        val uiAlbum = album?.let {
                            uiAlbumMapper(
                                count = 0,
                                imageCount = 0,
                                videoCount = 0,
                                cover = null,
                                defaultCover = null,
                                album = it,
                            )
                        }

                        _state.update {
                            it.copy(uiAlbum = uiAlbum)
                        }
                    }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    private fun observeAlbumPhotos(albumId: AlbumId, refresh: Boolean) {
        observeAlbumPhotosJob?.cancel()
        observeAlbumPhotosJob = viewModelScope.launch {
            runCatching {
                getAlbumPhotosUseCase(albumId, refresh)
                    .onEach { sourcePhotos = it }
                    .map(::filterNonSensitivePhotos)
                    .onEach(::updateSelection)
                    .collectLatest { photos ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                photos = photos,
                            )
                        }
                    }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    private fun filterNonSensitivePhotos(photos: List<Photo>): List<Photo> {
        val showHiddenItems = showHiddenItems ?: return photos
        val isPaid = _state.value.accountType?.isPaid ?: return photos

        return if (showHiddenItems || !isPaid || _state.value.isBusinessAccountExpired) {
            photos
        } else {
            photos.filter { !it.isSensitive && !it.isSensitiveInherited }
        }
    }

    private fun observePhotosRemovingProgress(albumId: AlbumId) {
        observeAlbumPhotosRemovingProgress(albumId)
            .onEach(::handlePhotosRemovingProgress)
            .launchIn(viewModelScope)
    }

    private fun handlePhotosRemovingProgress(progress: AlbumPhotosRemovingProgress?) {
        _state.update {
            it.copy(
                isRemovingPhotos = progress?.isProgressing ?: false,
                totalRemovedPhotos = progress?.totalRemovedPhotos ?: 0,
            )
        }
    }

    fun updatePhotosRemovingProgressCompleted(albumId: AlbumId) = viewModelScope.launch {
        updateAlbumPhotosRemovingProgressCompleted(albumId)
    }

    private fun observePhotosAddingProgress(albumId: AlbumId) {
        observeAlbumPhotosAddingProgress(albumId)
            .onEach(::handlePhotosAddingProgress)
            .launchIn(viewModelScope)
    }

    private fun handlePhotosAddingProgress(progress: AlbumPhotosAddingProgress?) {
        if (progress?.isProgressing == false && _state.value.photos.isEmpty()) {
            _state.update {
                it.copy(isLoading = true)
            }
            albumId?.let { observeAlbumPhotos(it, true) }
        }

        _state.update {
            it.copy(
                isAddingPhotos = progress?.isProgressing ?: false,
                totalAddedPhotos = progress?.totalAddedPhotos ?: 0,
                showProgressMessage = progress?.isAsync == false,
            )
        }
    }

    fun updatePhotosAddingProgressCompleted(albumId: AlbumId) = viewModelScope.launch {
        updateAlbumPhotosAddingProgressCompleted(albumId)
    }

    fun deleteAlbum() {
        _state.update {
            it.copy(isDeleteAlbum = true)
        }
    }

    fun showRemoveLinkConfirmation() {
        _state.update {
            it.copy(showRemoveLinkConfirmation = true)
        }
    }

    fun closeRemoveLinkConfirmation() {
        _state.update {
            it.copy(showRemoveLinkConfirmation = false)
        }
    }

    fun disableExportAlbum(albumId: AlbumId) = viewModelScope.launch {
        val numRemoved = disableExportAlbumsUseCase(albumIds = listOf(albumId))
        _state.update {
            it.copy(isLinkRemoved = numRemoved > 0)
        }
    }

    fun resetLinkRemoved() {
        _state.update {
            it.copy(isLinkRemoved = false)
        }
    }

    fun setCurrentSort(sort: Sort) {
        _state.update {
            it.copy(currentSort = sort)
        }
    }

    fun setCurrentMediaType(mediaType: FilterMediaType) {
        _state.update {
            it.copy(currentMediaType = mediaType)
        }
    }

    fun showSortByDialog(showSortByDialog: Boolean) {
        _state.update {
            it.copy(showSortByDialog = showSortByDialog)
        }
    }

    fun showFilterDialog(showFilterDialog: Boolean) {
        _state.update {
            it.copy(showFilterDialog = showFilterDialog)
        }
    }

    fun showRenameDialog(showRenameDialog: Boolean) {
        _state.update {
            it.copy(showRenameDialog = showRenameDialog)
        }
    }

    fun showDeleteAlbumsConfirmation() {
        _state.update {
            it.copy(showDeleteAlbumsConfirmation = true)
        }
    }

    fun closeDeleteAlbumsConfirmation() {
        _state.update {
            it.copy(showDeleteAlbumsConfirmation = false)
        }
    }

    fun setShowRemovePhotosFromAlbumDialog(show: Boolean) {
        _state.update {
            it.copy(showRemovePhotosDialog = show)
        }
    }

    fun removeFavourites() = viewModelScope.launch {
        val selectedPhotoIds = _state.value.selectedPhotos.map { NodeId(it.id) }
        removeFavouritesUseCase(selectedPhotoIds)

        _state.update {
            it.copy(selectedPhotos = emptySet())
        }
    }

    fun removePhotosFromAlbum() = viewModelScope.launch {
        val album = _state.value.uiAlbum?.id as? UserAlbum ?: return@launch
        _state.value.selectedPhotos.mapNotNull { photo ->
            photo.albumPhotoId?.let {
                AlbumPhotoId(
                    id = it,
                    nodeId = NodeId(photo.id),
                    albumId = album.id,
                )
            }
        }.also {
            removePhotosFromAlbumUseCase(albumId = album.id, photoIds = it)
        }
    }

    fun selectAllPhotos() {
        val photos = _state.value.photos
        val selectedPhotos = when (_state.value.currentMediaType) {
            FilterMediaType.ALL_MEDIA -> photos
            FilterMediaType.IMAGES -> photos.filterIsInstance<Photo.Image>()
            FilterMediaType.VIDEOS -> photos.filterIsInstance<Photo.Video>()
        }
        _state.update {
            it.copy(selectedPhotos = selectedPhotos.toMutableSet())
        }
    }

    fun clearSelectedPhotos() {
        _state.update {
            it.copy(selectedPhotos = emptySet())
        }
    }

    fun togglePhotoSelection(photo: Photo) {
        val selectedPhotos = _state.value.selectedPhotos.toMutableSet()
        if (photo in selectedPhotos) {
            Analytics.tracker.trackEvent(
                PhotoItemSelectedEvent(selectionType = PhotoItemSelected.SelectionType.MultiRemove)
            )
            selectedPhotos.remove(photo)
        } else {
            Analytics.tracker.trackEvent(
                PhotoItemSelectedEvent(selectionType = PhotoItemSelected.SelectionType.MultiAdd)
            )
            selectedPhotos.add(photo)
        }

        _state.update {
            it.copy(selectedPhotos = selectedPhotos)
        }
    }

    fun setSnackBarMessage(snackBarMessage: String) {
        _state.update {
            it.copy(snackBarMessage = snackBarMessage)
        }
    }

    suspend fun getSelectedNodes(): List<MegaNode> {
        return runCatching {
            val selectedPhotoIds = _state.value.selectedPhotos.map { it.id }
            getNodeListByIds(selectedPhotoIds)
        }.onFailure { Timber.e(it) }.getOrDefault(emptyList())
    }

    suspend fun getSelectedPhotos() = _state.value.selectedPhotos

    fun updateAlbumName(title: String, albumNames: List<String>) = viewModelScope.launch {
        runCatching {
            val finalTitle = title.trim()
            val proscribedAlbumNames = getProscribedAlbumNamesUseCase()

            if (checkTitleValidity(finalTitle, proscribedAlbumNames, albumNames)) {
                val albumId = (_state.value.uiAlbum?.id as? UserAlbum)?.id
                albumId?.let { updateAlbumNameUseCase(it, finalTitle) }

                _state.update {
                    it.copy(showRenameDialog = false)
                }
            }
        }.onFailure { exception ->
            Timber.e(exception)

            _state.update {
                it.copy(showRenameDialog = false)
            }
        }
    }

    private fun checkTitleValidity(
        title: String,
        proscribedAlbumNames: List<String>,
        albumNames: List<String>,
    ): Boolean {
        var errorMessage: Int? = null
        var isTitleValid = true

        if (title.isEmpty()) {
            isTitleValid = false
            errorMessage = R.string.invalid_string
        } else if (title.isEmpty() || proscribedAlbumNames.any { it.equals(title, true) }) {
            isTitleValid = false
            errorMessage = R.string.photos_create_album_error_message_systems_album
        } else if (title in albumNames) {
            isTitleValid = false
            errorMessage = R.string.photos_create_album_error_message_duplicate
        } else if ("[\\\\*/:<>?\"|]".toRegex().containsMatchIn(title)) {
            isTitleValid = false
            errorMessage = R.string.invalid_characters_defined
        }

        _state.update {
            it.copy(
                isInputNameValid = isTitleValid,
                createDialogErrorMessage = errorMessage,
                newAlbumTitleInput = title,
            )
        }

        return isTitleValid
    }

    fun revalidateAlbumNameInput(albumNames: List<String>) = viewModelScope.launch {
        if (!_state.value.isInputNameValid && _state.value.showRenameDialog) {
            checkTitleValidity(
                title = _state.value.newAlbumTitleInput,
                proscribedAlbumNames = getProscribedAlbumNamesUseCase(),
                albumNames = albumNames,
            )
        }
    }

    fun setNewAlbumNameValidity(valid: Boolean) = _state.update {
        it.copy(isInputNameValid = valid)
    }

    fun hideOrUnhideNodes(hide: Boolean) {
        val photoIds = _state.value.selectedPhotos.map { it.id }
        viewModelScope.launch {
            for (id in photoIds) {
                async {
                    runCatching {
                        updateNodeSensitiveUseCase(nodeId = NodeId(id), isSensitive = hide)
                    }.onFailure { Timber.e("Update sensitivity failed: $it") }
                }
            }
        }
    }

    fun setHiddenNodesOnboarded() {
        _state.update {
            it.copy(isHiddenNodesOnboarded = true)
        }
    }
}
