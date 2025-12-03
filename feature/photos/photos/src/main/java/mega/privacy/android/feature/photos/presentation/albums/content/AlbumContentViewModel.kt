package mega.privacy.android.feature.photos.presentation.albums.content

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.CoroutineDispatcher
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
import kotlinx.coroutines.withContext
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.media.MediaAlbum
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.Album.FavouriteAlbum
import mega.privacy.android.domain.entity.photos.Album.GifAlbum
import mega.privacy.android.domain.entity.photos.Album.RawAlbum
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.AlbumPhotoId
import mega.privacy.android.domain.entity.photos.AlbumPhotosAddingProgress
import mega.privacy.android.domain.entity.photos.AlbumPhotosRemovingProgress
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.exception.account.AlbumNameValidationException
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.usecase.GetAlbumPhotosUseCase
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.GetDefaultAlbumPhotos
import mega.privacy.android.domain.usecase.GetNodeListByIdsUseCase
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.domain.usecase.ObserveAlbumPhotosAddingProgress
import mega.privacy.android.domain.usecase.ObserveAlbumPhotosRemovingProgress
import mega.privacy.android.domain.usecase.UpdateAlbumPhotosAddingProgressCompleted
import mega.privacy.android.domain.usecase.UpdateAlbumPhotosRemovingProgressCompleted
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.favourites.RemoveFavouritesUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.media.GetUserAlbumCoverPhotoUseCase
import mega.privacy.android.domain.usecase.media.MonitorUserAlbumByIdUseCase
import mega.privacy.android.domain.usecase.media.ValidateAndUpdateUserAlbumUseCase
import mega.privacy.android.domain.usecase.photos.DisableExportAlbumsUseCase
import mega.privacy.android.domain.usecase.photos.GetDefaultAlbumsMapUseCase
import mega.privacy.android.domain.usecase.photos.RemoveAlbumsUseCase
import mega.privacy.android.domain.usecase.photos.RemovePhotosFromAlbumUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.feature.photos.mapper.AlbumNameValidationExceptionMessageMapper
import mega.privacy.android.feature.photos.mapper.AlbumUiStateMapper
import mega.privacy.android.feature.photos.mapper.LegacyMediaSystemAlbumMapper
import mega.privacy.android.feature.photos.mapper.LegacyPhotosSortMapper
import mega.privacy.android.feature.photos.mapper.PhotoUiStateMapper
import mega.privacy.android.feature.photos.model.AlbumSortConfiguration
import mega.privacy.android.feature.photos.model.FilterMediaType
import mega.privacy.android.feature.photos.model.PhotoUiState
import mega.privacy.android.feature.photos.presentation.albums.content.model.AlbumContentSelectionAction
import mega.privacy.android.feature.photos.presentation.albums.model.FavouriteSystemAlbum
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.navigation.destination.AlbumContentNavKey
import mega.privacy.android.navigation.destination.AlbumContentPreviewNavKey
import mega.privacy.android.shared.resources.R as sharedResR
import mega.privacy.mobile.analytics.event.AlbumContentDeleteAlbumEvent
import mega.privacy.mobile.analytics.event.PhotoItemSelected
import mega.privacy.mobile.analytics.event.PhotoItemSelectedEvent
import timber.log.Timber

@HiltViewModel(assistedFactory = AlbumContentViewModel.Factory::class)
class AlbumContentViewModel @AssistedInject constructor(
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    private val savedStateHandle: SavedStateHandle,
    private val getDefaultAlbumPhotos: GetDefaultAlbumPhotos,
    private val getDefaultAlbumsMapUseCase: GetDefaultAlbumsMapUseCase,
    private val getUserAlbum: MonitorUserAlbumByIdUseCase,
    private val getAlbumPhotosUseCase: GetAlbumPhotosUseCase,
    private val albumUiStateMapper: AlbumUiStateMapper,
    private val legacyMediaSystemAlbumMapper: LegacyMediaSystemAlbumMapper,
    private val observeAlbumPhotosAddingProgress: ObserveAlbumPhotosAddingProgress,
    private val updateAlbumPhotosAddingProgressCompleted: UpdateAlbumPhotosAddingProgressCompleted,
    private val observeAlbumPhotosRemovingProgress: ObserveAlbumPhotosRemovingProgress,
    private val updateAlbumPhotosRemovingProgressCompleted: UpdateAlbumPhotosRemovingProgressCompleted,
    private val disableExportAlbumsUseCase: DisableExportAlbumsUseCase,
    private val removeFavouritesUseCase: RemoveFavouritesUseCase,
    private val removePhotosFromAlbumUseCase: RemovePhotosFromAlbumUseCase,
    private val getNodeListByIdsUseCase: GetNodeListByIdsUseCase,
    private val updateAlbumNameUseCase: ValidateAndUpdateUserAlbumUseCase,
    private val updateNodeSensitiveUseCase: UpdateNodeSensitiveUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val isHiddenNodesOnboardedUseCase: IsHiddenNodesOnboardedUseCase,
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase,
    private val photoUiStateMapper: PhotoUiStateMapper,
    private val getUserAlbumCoverPhotoUseCase: GetUserAlbumCoverPhotoUseCase,
    private val removeAlbumsUseCase: RemoveAlbumsUseCase,
    private val snackbarEventQueue: SnackbarEventQueue,
    private val albumNameValidationExceptionMessageMapper: AlbumNameValidationExceptionMessageMapper,
    private val monitorThemeModeUseCase: MonitorThemeModeUseCase,
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    private val legacyPhotosSortMapper: LegacyPhotosSortMapper,
    @Assisted private val navKey: AlbumContentNavKey?,
) : ViewModel() {
    private val _state = MutableStateFlow(AlbumContentUiState())
    val state = _state.asStateFlow()

    private var observeAlbumPhotosJob: Job? = null

    var sourcePhotos: List<Photo>? = null
        private set

    private var showHiddenItems: Boolean? = null

    private val albumType: String?
        get() = savedStateHandle["type"] ?: navKey?.type

    private val albumId: AlbumId?
        get() = (savedStateHandle["id"] ?: navKey?.id)
            ?.let { AlbumId(it) }

    private val photosFetchers: Map<String, () -> Unit> = mapOf(
        "favourite" to { fetchSystemPhotos(systemAlbum = FavouriteAlbum) },
        "gif" to { fetchSystemPhotos(systemAlbum = GifAlbum) },
        "raw" to { fetchSystemPhotos(systemAlbum = RawAlbum) },
        "custom" to { fetchAlbumPhotos() },
    )

    init {
        monitorThemeMode()
        fetchPhotos()
        fetchIsHiddenNodesOnboarded()

        monitorShowHiddenItems()
        monitorAccountDetail()
    }

    private fun monitorThemeMode() {
        monitorThemeModeUseCase()
            .onEach { mode ->
                _state.update { it.copy(themeMode = mode) }
            }
            .launchIn(viewModelScope)
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

    private fun updateSelection(photos: List<PhotoUiState>) {
        val selectedPhotos = _state.value.selectedPhotos.filter { selectedPhoto ->
            photos.any { it.id == selectedPhoto.id }
        }.toSet()

        _state.update {
            it.copy(selectedPhotos = selectedPhotos.toImmutableSet())
        }

        updateBottomBarActionVisibility()
    }

    /**
     * Updates the visibility of bottom bar actions based on:
     * - Album type (FavouriteAlbum shows RemoveFavourites, UserAlbum shows Delete)
     * - Selected photos' sensitive state (for Hide/Unhide actions)
     * - Account type (paid/free)
     * - Business account expired status
     * - Hidden nodes onboarded status
     */
    private fun updateBottomBarActionVisibility() {
        viewModelScope.launch(defaultDispatcher) {
            val state = _state.value
            val selectedPhotos = state.selectedPhotos

            if (selectedPhotos.isEmpty()) {
                _state.update {
                    it.copy(visibleBottomBarActions = persistentListOf())
                }
                return@launch
            }

            val mediaAlbum = state.uiAlbum?.mediaAlbum
            val isPaid = state.accountType?.isPaid ?: false
            val isBusinessAccountExpired = state.isBusinessAccountExpired
            val isHiddenNodesOnboarded = state.isHiddenNodesOnboarded

            // Hide/Unhide visibility logic
            val includeSensitiveInheritedNode = selectedPhotos.any { it.isSensitiveInherited }
            val hasNonSensitiveNode = selectedPhotos.any { !it.isSensitive }

            val showHide = !isPaid ||
                    isBusinessAccountExpired ||
                    (hasNonSensitiveNode && isHiddenNodesOnboarded != null && !includeSensitiveInheritedNode)
            val showUnhide = isPaid &&
                    !isBusinessAccountExpired &&
                    !hasNonSensitiveNode &&
                    !includeSensitiveInheritedNode

            // Delete (Remove Photos) - only visible for UserAlbum
            val showDelete = mediaAlbum is MediaAlbum.User

            // RemoveFavourites - only visible for FavouriteAlbum
            val showRemoveFavourites =
                (mediaAlbum as? MediaAlbum.System)?.id is FavouriteSystemAlbum

            val visibleActions = AlbumContentSelectionAction
                .bottomBarItems
                .filter { action ->
                    when (action) {
                        is AlbumContentSelectionAction.Hide -> showHide
                        is AlbumContentSelectionAction.Unhide -> showUnhide
                        is AlbumContentSelectionAction.Delete -> showDelete
                        is AlbumContentSelectionAction.RemoveFavourites -> showRemoveFavourites
                        else -> true
                    }
                }

            _state.update {
                it.copy(visibleBottomBarActions = visibleActions.toImmutableList())
            }
        }
    }

    private fun fetchSystemPhotos(systemAlbum: Album) {
        viewModelScope.launch {
            val filter = getDefaultAlbumsMapUseCase()[systemAlbum] ?: return@launch
            val isPaginationEnabled = runCatching {
                getFeatureFlagValueUseCase(AppFeatures.TimelinePhotosPagination)
            }.getOrDefault(false)

            runCatching {
                // Will be refactored later in the next phase
                val albumCover = albumId?.let { getUserAlbumCoverPhotoUseCase(it) }

                getDefaultAlbumPhotos(isPaginationEnabled, listOf(filter))
                    .onEach { sourcePhotos = it }
                    .map(::filterNonSensitivePhotos)
                    .onEach(::updateSelection)
                    .collectLatest { photos ->
                        val mediaSystemAlbum = legacyMediaSystemAlbumMapper(
                            album = systemAlbum,
                            cover = albumCover
                        )
                        _state.update {
                            it.copy(
                                isLoading = false,
                                photos = photos,
                                uiAlbum = mediaSystemAlbum?.let { album ->
                                    albumUiStateMapper(album)
                                }
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
                        val uiAlbum = album?.let(albumUiStateMapper::invoke)

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

    private fun filterNonSensitivePhotos(photos: List<Photo>): ImmutableList<PhotoUiState> {
        val photosUiState = photos.map { photoUiStateMapper(it) }
        val showHiddenItems = showHiddenItems ?: return photosUiState.toImmutableList()
        val isPaid = _state.value.accountType?.isPaid ?: return photosUiState.toImmutableList()
        val filteredPhotos =
            if (showHiddenItems || !isPaid || _state.value.isBusinessAccountExpired) {
                photosUiState
            } else {
                photosUiState.filter { !it.isSensitive && !it.isSensitiveInherited }
            }

        return filteredPhotos.toImmutableList()
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

    fun updatePhotosRemovingProgressCompleted(albumId: AlbumId) {
        viewModelScope.launch {
            updateAlbumPhotosRemovingProgressCompleted(albumId)
        }
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

    fun updatePhotosAddingProgressCompleted(albumId: AlbumId) {
        viewModelScope.launch {
            updateAlbumPhotosAddingProgressCompleted(albumId)
        }
    }

    internal fun deleteAlbum() {
        viewModelScope.launch {
            runCatching {
                val albumIdToRemove = albumId
                checkNotNull(albumIdToRemove)
                removeAlbumsUseCase(listOf(albumIdToRemove))
            }.onFailure {
                Timber.e(it)
            }.onSuccess {
                snackbarEventQueue.queueMessage(
                    sharedResR.string.delete_singular_album_confirmation_message,
                    state.value.uiAlbum?.title.orEmpty()
                )
                _state.update {
                    it.copy(deleteAlbumSuccessEvent = triggered)
                }
            }
        }
    }

    internal fun resetDeleteAlbumSuccess() {
        _state.update {
            it.copy(deleteAlbumSuccessEvent = consumed)
        }
    }

    internal fun resetShowDeleteConfirmation() {
        _state.update {
            it.copy(showDeleteAlbumConfirmation = consumed)
        }
    }

    private fun getManageLinkEvent(): ManageLinkEvent? {
        val userAlbum = (_state.value.uiAlbum?.mediaAlbum as? MediaAlbum.User) ?: return null
        val hasSensitiveMedia = sourcePhotos
            ?.any { it.isSensitive || it.isSensitiveInherited }
            ?: false

        return ManageLinkEvent(
            album = userAlbum,
            hasSensitiveContent = hasSensitiveMedia
        )
    }

    fun resetManageLink() {
        _state.update {
            it.copy(manageLinkEvent = consumed())
        }
    }

    fun resetRemoveLinkConfirmation() {
        _state.update {
            it.copy(showRemoveLinkConfirmation = consumed)
        }
    }

    fun disableExportAlbum() {
        viewModelScope.launch {
            runCatching {
                val userAlbum = (_state.value.uiAlbum?.mediaAlbum as? MediaAlbum.User)
                checkNotNull(userAlbum)
                disableExportAlbumsUseCase(albumIds = listOf(userAlbum.id))
            }.onSuccess { linkRemoved ->
                _state.update {
                    it.copy(
                        linkRemovedSuccessEvent = if (linkRemoved > 0) triggered else consumed
                    )
                }
            }
        }
    }

    fun resetLinkRemovedSuccess() {
        _state.update {
            it.copy(linkRemovedSuccessEvent = consumed)
        }
    }

    fun sortPhotos(sortConfiguration: AlbumSortConfiguration) {
        viewModelScope.launch {
            val currentPhotos = _state.value.photos
            val sortedPhotosUiState = withContext(defaultDispatcher) {
                val comparator = if (sortConfiguration.sortDirection == SortDirection.Ascending) {
                    compareBy<PhotoUiState> { it.modificationTime }
                } else {
                    compareByDescending { it.modificationTime }
                }.thenByDescending { it.id }

                currentPhotos.sortedWith(comparator).toImmutableList()
            }

            _state.update {
                it.copy(
                    albumSortConfiguration = sortConfiguration,
                    photos = sortedPhotosUiState
                )
            }
        }
    }

    fun previewPhoto(photo: PhotoUiState) {
        viewModelScope.launch {
            val args = AlbumContentPreviewNavKey(
                albumId = (_state.value.uiAlbum?.mediaAlbum as? MediaAlbum.User)?.id?.id,
                photoId = photo.id,
                albumType = albumType.orEmpty(),
                sortType = legacyPhotosSortMapper(_state.value.albumSortConfiguration.sortDirection).name,
                title = _state.value.uiAlbum?.title.orEmpty()
            )

            _state.update { it.copy(previewAlbumContentEvent = triggered(args)) }
        }
    }

    fun resetPreviewPhoto() {
        _state.update { it.copy(previewAlbumContentEvent = consumed()) }
    }

    fun removeFavourites() = viewModelScope.launch {
        val selectedPhotoIds = _state.value.selectedPhotos.map { NodeId(it.id) }
        removeFavouritesUseCase(selectedPhotoIds)

        _state.update {
            it.copy(selectedPhotos = persistentSetOf())
        }
    }

    fun removePhotosFromAlbum() = viewModelScope.launch {
        val album = _state.value.uiAlbum?.mediaAlbum as? MediaAlbum.User ?: return@launch
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

    internal fun savePhotosToDevice() {
        fetchNodesAndExecute { nodes ->
            _state.update { it.copy(savePhotosToDeviceEvent = triggered(nodes)) }
        }
    }

    internal fun resetSavePhotosToDevice() {
        _state.update { it.copy(savePhotosToDeviceEvent = consumed()) }
    }

    internal fun sharePhotos() {
        fetchNodesAndExecute { nodes ->
            _state.update { it.copy(sharePhotosEvent = triggered(nodes)) }
        }
    }

    internal fun resetSharePhotos() {
        _state.update { it.copy(sharePhotosEvent = consumed()) }
    }

    internal fun sendPhotosToChat() {
        fetchNodesAndExecute { nodes ->
            _state.update { it.copy(sendPhotosToChatEvent = triggered(nodes)) }
        }
    }

    internal fun resetSendPhotosToChat() {
        _state.update { it.copy(sendPhotosToChatEvent = consumed()) }
    }

    private fun fetchNodesAndExecute(block: (List<TypedNode>) -> Unit) {
        viewModelScope.launch {
            runCatching {
                val selectedPhotoIds = _state.value.selectedPhotos.map { NodeId(it.id) }
                getNodeListByIdsUseCase(selectedPhotoIds)
            }.onSuccess { nodes ->
                if (nodes.isNotEmpty()) {
                    block(nodes)
                }
            }
        }
    }

    fun selectAllPhotos() {
        val photos = _state.value.photos
        val selectedPhotos = when (_state.value.currentMediaType) {
            FilterMediaType.ALL_MEDIA -> photos
            FilterMediaType.IMAGES -> photos.filterIsInstance<PhotoUiState.Image>()
            FilterMediaType.VIDEOS -> photos.filterIsInstance<PhotoUiState.Video>()
        }
        _state.update {
            it.copy(selectedPhotos = selectedPhotos.toImmutableSet())
        }
        updateBottomBarActionVisibility()
    }

    fun clearSelectedPhotos() {
        _state.update {
            it.copy(selectedPhotos = persistentSetOf())
        }

        updateBottomBarActionVisibility()
    }

    fun togglePhotoSelection(photo: PhotoUiState) {
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
            it.copy(selectedPhotos = selectedPhotos.toImmutableSet())
        }
        updateBottomBarActionVisibility()
    }

    fun updateAlbumName(name: String) = viewModelScope.launch {
        runCatching {
            val albumId = (_state.value.uiAlbum?.mediaAlbum as? MediaAlbum.User)?.id
            albumId?.let { updateAlbumNameUseCase(it, name.trim()) }
        }.onFailure { e ->
            Timber.e(e)

            if (e is AlbumNameValidationException) {
                val message = albumNameValidationExceptionMessageMapper(e)
                _state.update {
                    it.copy(updateAlbumNameErrorMessage = triggered(message))
                }
            }
        }.onSuccess {
            resetShowUpdateAlbumName()
        }
    }

    fun resetShowUpdateAlbumName() {
        _state.update {
            it.copy(showUpdateAlbumName = consumed)
        }
    }

    fun resetUpdateAlbumNameErrorMessage() {
        _state.update {
            it.copy(updateAlbumNameErrorMessage = consumed())
        }
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

    fun resetSelectAlbumCoverEvent() {
        _state.update {
            it.copy(selectAlbumCoverEvent = consumed())
        }
    }

    fun handleAction(action: AlbumContentSelectionAction) {
        viewModelScope.launch {
            validateStorageState {
                when (action) {
                    is AlbumContentSelectionAction.Rename -> {
                        _state.update {
                            it.copy(showUpdateAlbumName = triggered)
                        }
                    }

                    is AlbumContentSelectionAction.SelectAlbumCover -> {
                        val userAlbum = (_state.value.uiAlbum?.mediaAlbum as? MediaAlbum.User)
                        _state.update {
                            it.copy(selectAlbumCoverEvent = triggered(userAlbum?.id))
                        }
                    }

                    is AlbumContentSelectionAction.ManageLink -> {
                        _state.update {
                            it.copy(manageLinkEvent = triggered(getManageLinkEvent()))
                        }
                    }

                    is AlbumContentSelectionAction.RemoveLink -> {
                        _state.update {
                            it.copy(showRemoveLinkConfirmation = triggered)
                        }
                    }

                    is AlbumContentSelectionAction.Delete -> {
                        if (_state.value.photos.isEmpty()) {
                            Analytics.tracker.trackEvent(AlbumContentDeleteAlbumEvent)
                            deleteAlbum()
                        } else {
                            _state.update {
                                it.copy(showDeleteAlbumConfirmation = triggered)
                            }
                        }
                    }

                    is AlbumContentSelectionAction.AddItems -> {
                        _state.update {
                            it.copy(addMoreItemsEvent = triggered)
                        }
                    }

                    else -> {
                        Timber.d("handleBottomSheetAction: Unknown action $action")
                    }
                }
            }
        }
    }

    private fun validateStorageState(block: () -> Unit) {
        runCatching {
            monitorStorageStateEventUseCase().value.storageState
        }.onSuccess { storageState ->
            if (storageState == StorageState.PayWall) {
                _state.update { it.copy(paywallEvent = triggered) }
            } else {
                block()
            }
        }.onFailure {
            Timber.e("validateStorageState: $it")
        }
    }

    fun resetPaywallEvent() {
        _state.update {
            it.copy(paywallEvent = consumed)
        }
    }

    fun resetAddMoreItems() {
        _state.update {
            it.copy(addMoreItemsEvent = consumed)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(navKey: AlbumContentNavKey?): AlbumContentViewModel
    }
}