package mega.privacy.android.app.presentation.photos.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.R
import mega.privacy.android.app.domain.usecase.GetNodeListByIds
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.photos.albums.model.AlbumTitle
import mega.privacy.android.app.presentation.photos.albums.model.AlbumsViewState
import mega.privacy.android.app.presentation.photos.albums.model.UIAlbum
import mega.privacy.android.app.presentation.photos.albums.model.getAlbumPhotos
import mega.privacy.android.app.presentation.photos.albums.model.mapper.LegacyUIAlbumMapper
import mega.privacy.android.app.presentation.photos.albums.model.mapper.UIAlbumMapper
import mega.privacy.android.app.presentation.photos.model.FilterMediaType
import mega.privacy.android.app.presentation.photos.model.Sort
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.AlbumPhotoId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.usecase.GetAlbumPhotos
import mega.privacy.android.domain.usecase.GetDefaultAlbumPhotos
import mega.privacy.android.domain.usecase.GetUserAlbums
import mega.privacy.android.domain.usecase.favourites.RemoveFavouritesUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.photos.CreateAlbumUseCase
import mega.privacy.android.domain.usecase.photos.DisableExportAlbumsUseCase
import mega.privacy.android.domain.usecase.photos.GetDefaultAlbumsMapUseCase
import mega.privacy.android.domain.usecase.photos.GetNextDefaultAlbumNameUseCase
import mega.privacy.android.domain.usecase.photos.GetProscribedAlbumNamesUseCase
import mega.privacy.android.domain.usecase.photos.RemoveAlbumsUseCase
import mega.privacy.android.domain.usecase.photos.RemovePhotosFromAlbumUseCase
import mega.privacy.android.domain.usecase.photos.UpdateAlbumNameUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * AlbumsViewModel handle albums cover page view logic
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AlbumsViewModel @Inject constructor(
    private val getDefaultAlbumPhotos: GetDefaultAlbumPhotos,
    private val getDefaultAlbumsMapUseCase: GetDefaultAlbumsMapUseCase,
    private val getUserAlbums: GetUserAlbums,
    private val getAlbumPhotos: GetAlbumPhotos,
    private val getProscribedAlbumNamesUseCase: GetProscribedAlbumNamesUseCase,
    private val uiAlbumMapper: UIAlbumMapper,
    private val legacyUIAlbumMapper: LegacyUIAlbumMapper,
    private var getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val removeFavouritesUseCase: RemoveFavouritesUseCase,
    private val getNodeListByIds: GetNodeListByIds,
    private val createAlbumUseCase: CreateAlbumUseCase,
    private val removeAlbumsUseCase: RemoveAlbumsUseCase,
    private val removePhotosFromAlbumUseCase: RemovePhotosFromAlbumUseCase,
    private val updateAlbumNameUseCase: UpdateAlbumNameUseCase,
    private val getNextDefaultAlbumNameUseCase: GetNextDefaultAlbumNameUseCase,
    private val disableExportAlbumsUseCase: DisableExportAlbumsUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _state = MutableStateFlow(AlbumsViewState())
    val state = _state.asStateFlow()
    private var currentNodeJob: Job? = null

    private var albumJob: Job? = null
    private val albumJobs: MutableMap<AlbumId, Job> = mutableMapOf()

    private var createAlbumJob: Job? = null

    @Volatile
    var isReworkAlbum: Boolean = false

    init {
        viewModelScope.launch {
            isReworkAlbum = getFeatureFlagValueUseCase(AppFeatures.ReworkAlbum)
            if (!isReworkAlbum) {
                loadAlbumsInLegacyArchitecture()
            } else {
                loadAlbums()
            }
        }
    }

    private fun loadAlbumsInLegacyArchitecture() {
        currentNodeJob = viewModelScope.launch {
            val includedSystemAlbums = getDefaultAlbumsMapUseCase()
            runCatching {
                getDefaultAlbumPhotos(
                    includedSystemAlbums.values.toList()
                ).mapLatest { photos ->
                    includedSystemAlbums.mapNotNull { (key, value) ->
                        photos.filter {
                            value(it)
                        }.takeIf {
                            shouldAddAlbum(it, key)
                        }?.let {
                            legacyUIAlbumMapper(it, key, isLoadingDone = true)
                        }
                    }
                }.collectLatest { systemAlbums ->
                    albumJob ?: loadUserAlbums()

                    _state.update { state ->
                        val albums = withContext(defaultDispatcher) {
                            val userAlbums = state.albums.filter { it.id is Album.UserAlbum }
                            systemAlbums + userAlbums
                        }
                        val currentAlbum = checkCurrentAlbumExists(albums = albums)
                        state.copy(
                            albums = albums,
                            currentAlbum = currentAlbum,
                        )
                    }
                }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    private fun loadAlbums() {
        currentNodeJob = viewModelScope.launch {
            val includedSystemAlbums = getDefaultAlbumsMapUseCase()
            runCatching {
                getDefaultAlbumPhotos(
                    includedSystemAlbums.values.toList()
                ).mapLatest { photos ->
                    includedSystemAlbums.mapNotNull { (key, value) ->
                        photos.filter {
                            value(it)
                        }.takeIf {
                            shouldAddAlbum(it, key)
                        }?.let {
                            val cover = withContext(defaultDispatcher) {
                                it.maxByOrNull { photo -> photo.modificationTime }
                            }
                            uiAlbumMapper(it.size, cover, cover, key)
                        }
                    }
                }.collectLatest { systemAlbums ->
                    albumJob ?: loadUserAlbums()

                    val albums = withContext(defaultDispatcher) {
                        val userAlbums = _state.value.albums.filter { it.id is Album.UserAlbum }
                        systemAlbums + userAlbums
                    }

                    _state.update {
                        it.copy(albums = albums)
                    }
                }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    /**
     * User albums with real-time updates
     */
    private fun loadUserAlbums() {
        albumJob?.cancel()
        albumJob = viewModelScope.launch {
            if (!getFeatureFlagValueUseCase(AppFeatures.UserAlbums)) {
                _state.update {
                    it.copy(showAlbums = true)
                }
                return@launch
            }

            getUserAlbums()
                .catch { exception -> Timber.e(exception) }
                .mapLatest(::filterActiveAlbums)
                .collectLatest(::handleUserAlbums)
        }
    }

    private suspend fun filterActiveAlbums(albums: List<Album.UserAlbum>) =
        withContext(defaultDispatcher) {
            albums.filter { album -> album.id !in _state.value.deletedAlbumIds }
        }

    private suspend fun handleUserAlbums(userAlbums: List<Album.UserAlbum>) {
        _state.update { state ->
            val albums = updateUserAlbums(state.albums, userAlbums)
            val currentAlbumId = checkCurrentAlbumExists(albums = albums)

            state.copy(
                albums = albums,
                currentAlbum = currentAlbumId,
                showAlbums = true,
            )
        }

        for (album in userAlbums) {
            val job = albumJobs[album.id] ?: loadAlbumPhotos(album)
            albumJobs[album.id] = job
        }

        val activeAlbumIds = userAlbums.map { it.id }
        for (albumId in albumJobs.keys - activeAlbumIds) {
            albumJobs[albumId]?.cancel()
            albumJobs.remove(albumId)
        }
    }

    private suspend fun updateUserAlbums(
        uiAlbums: List<UIAlbum>,
        userAlbums: List<Album.UserAlbum>,
    ): List<UIAlbum> = withContext(defaultDispatcher) {
        val (systemUIAlbums, userUIAlbums) = uiAlbums.partition {
            it.id !is Album.UserAlbum
        }

        val updatedUserUIAlbums = userAlbums.map { userAlbum ->
            val uiAlbum = userUIAlbums.firstOrNull { uiAlbum ->
                (uiAlbum.id as? Album.UserAlbum)?.id == userAlbum.id
            }
            if (!isReworkAlbum) {
                legacyUIAlbumMapper(uiAlbum?.photos.orEmpty(), userAlbum, isLoadingDone = true)
            } else {
                val cover = userAlbum.cover
                val defaultCover = uiAlbum?.photos?.maxByOrNull { it.modificationTime }
                uiAlbumMapper(uiAlbum?.count ?: 0, cover, defaultCover, userAlbum)
            }
        }.sortedByDescending { (it.id as? Album.UserAlbum)?.creationTime }

        systemUIAlbums + updatedUserUIAlbums
    }

    private fun loadAlbumPhotos(album: Album.UserAlbum): Job = viewModelScope.launch {
        getAlbumPhotos(album.id)
            .catch { exception -> Timber.e(exception) }
            .collectLatest { photos -> handleAlbumPhotos(album, photos) }
    }

    private suspend fun handleAlbumPhotos(album: Album.UserAlbum, photos: List<Photo>) {
        _state.update { state ->
            val albums = updateAlbumPhotos(state.albums, album, photos)
            val currentAlbumId = checkCurrentAlbumExists(albums = albums)

            state.copy(
                albums = albums,
                currentAlbum = currentAlbumId,
            )
        }
    }

    private suspend fun updateAlbumPhotos(
        uiAlbums: List<UIAlbum>,
        userAlbum: Album.UserAlbum,
        photos: List<Photo>,
    ): List<UIAlbum> = withContext(defaultDispatcher) {
        val (systemUIAlbums, userUIAlbums) = uiAlbums.partition {
            it.id !is Album.UserAlbum
        }

        val updatedUserUIAlbums = userUIAlbums.map { uiAlbum ->
            if ((uiAlbum.id as? Album.UserAlbum)?.id == userAlbum.id) {
                if (!isReworkAlbum) {
                    legacyUIAlbumMapper(photos, uiAlbum.id, isLoadingDone = true)
                } else {
                    val cover = uiAlbum.id.cover
                    val defaultCover = photos.maxByOrNull { it.modificationTime }
                    uiAlbumMapper(photos.size, cover, defaultCover, uiAlbum.id)
                }
            } else {
                uiAlbum
            }
        }.sortedByDescending { (it.id as? Album.UserAlbum)?.creationTime }

        systemUIAlbums + updatedUserUIAlbums
    }

    fun deleteAlbums(albumIds: List<AlbumId>) {
        if (albumIds.isEmpty()) return

        removeAlbumIds(albumIds)
        clearAlbumSelection()
        updateInActiveAlbums(albumIds)
        loadUserAlbums()
    }

    /**
     * Remove Album Links
     */
    fun removeAlbumsLinks() = viewModelScope.launch {
        val removedLinksCount = disableExportAlbumsUseCase(_state.value.selectedAlbumIds.toList())
        _state.update {
            it.copy(
                removedLinksCount = removedLinksCount,
                showRemoveAlbumLinkDialog = false,
                selectedAlbumIds = setOf()
            )
        }
    }

    private fun removeAlbumIds(albumIds: List<AlbumId>) = viewModelScope.launch {
        try {
            removeAlbumsUseCase(albumIds)
        } catch (exception: Exception) {
            Timber.e(exception)
        }
    }

    private fun updateInActiveAlbums(albumIds: List<AlbumId>) {
        _state.update { state ->
            state.copy(deletedAlbumIds = state.deletedAlbumIds + albumIds)
        }
    }

    fun updateAlbumDeletedMessage(message: String) {
        _state.update {
            it.copy(albumDeletedMessage = message)
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

    /**
     * Show the Remove Link confirmation dialog
     */
    fun showRemoveLinkDialog() {
        _state.update {
            it.copy(showRemoveAlbumLinkDialog = true)
        }
    }

    /**
     * Close the Remove Link confirmation dialog
     */
    fun hideRemoveLinkDialog() {
        _state.update {
            it.copy(showRemoveAlbumLinkDialog = false)
        }
    }

    /**
     * Reset the Removed Links Count state value
     */
    fun resetRemovedLinksCount() {
        _state.update {
            it.copy(removedLinksCount = 0)
        }
    }

    fun selectAlbum(album: Album.UserAlbum) = viewModelScope.launch {
        _state.update {
            val selectedAlbumIds = withContext(defaultDispatcher) {
                it.selectedAlbumIds + album.id
            }
            it.copy(selectedAlbumIds = selectedAlbumIds)
        }
    }

    fun unselectAlbum(album: Album.UserAlbum) = viewModelScope.launch {
        _state.update {
            val selectedAlbumIds = withContext(defaultDispatcher) {
                it.selectedAlbumIds - album.id
            }
            it.copy(selectedAlbumIds = selectedAlbumIds)
        }
    }

    fun selectAllAlbums() = viewModelScope.launch {
        _state.update {
            val selectedAlbumIds = withContext(defaultDispatcher) {
                it.albums.mapNotNull { album -> (album.id as? Album.UserAlbum)?.id }.toSet()
            }
            it.copy(selectedAlbumIds = selectedAlbumIds)
        }
    }

    fun clearAlbumSelection() {
        _state.update {
            it.copy(selectedAlbumIds = setOf())
        }
    }

    /**
     * Create a new album
     *
     * @param title the name of the album
     */
    fun createNewAlbum(title: String) {
        if (createAlbumJob?.isActive == true) return
        createAlbumJob = viewModelScope.launch {
            try {
                val finalTitle = title.ifEmpty {
                    _state.value.createAlbumPlaceholderTitle
                }.trim()
                if (checkTitleValidity(finalTitle)) {
                    val album = createAlbumUseCase(finalTitle)
                    _state.update {
                        it.copy(
                            currentAlbum = album,
                            isAlbumCreatedSuccessfully = true,
                            newAlbumTitleInput = "",
                            showCreateAlbumDialog = false
                        )
                    }
                    Timber.d("Current album: ${album.title}")
                }
            } catch (exception: Exception) {
                Timber.e(exception)
                _state.update {
                    it.copy(isAlbumCreatedSuccessfully = false)
                }
            }
        }
    }

    fun updateAlbumName(title: String) = viewModelScope.launch {
        runCatching {
            val finalTitle = title.trim()
            if (checkTitleValidity(finalTitle)) {
                val currentAlbumId = (_state.value.currentAlbum as Album.UserAlbum).id
                updateAlbumNameUseCase(currentAlbumId, finalTitle)
                _state.update { state -> state.copy(showRenameDialog = false) }
            }
        }.onFailure {
            Timber.e(it)
            _state.update { state -> state.copy(showRenameDialog = false) }
        }
    }

    private fun checkCurrentAlbumExists(albums: List<UIAlbum>): Album? {
        val currentAlbum = _state.value.currentAlbum ?: return null
        return if (currentAlbum !is Album.UserAlbum) {
            albums.find { album -> album.id == currentAlbum }?.id
        } else {
            albums.find { album -> (album.id as? Album.UserAlbum)?.id == currentAlbum.id }?.id
        }
    }

    /**
     * Get the default album title
     */
    fun setPlaceholderAlbumTitle(placeholderTitle: String) {
        _state.update {
            it.copy(
                createAlbumPlaceholderTitle = getNextDefaultAlbumNameUseCase(
                    defaultName = placeholderTitle,
                    currentNames = getAllUserAlbumsNames()
                )
            )
        }
    }

    internal fun getAllUserAlbumsNames() = _state.value.albums.filter {
        it.id is Album.UserAlbum
    }.map { it.title }
        .filterIsInstance<AlbumTitle.StringTitle>().map { it.title }

    private suspend fun checkTitleValidity(
        title: String,
    ): Boolean {
        val proscribedStrings = getProscribedAlbumNamesUseCase()

        var errorMessage: Int? = null
        var isTitleValid = true

        if (title.isEmpty()) {
            isTitleValid = false
            errorMessage = R.string.invalid_string
        } else if (title.isEmpty() || proscribedStrings.any { it.equals(title, true) }) {
            isTitleValid = false
            errorMessage = R.string.photos_create_album_error_message_systems_album
        } else if (title in getAllUserAlbumsNames()) {
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

    private fun checkLatestNameInputValidity() = viewModelScope.launch {
        checkTitleValidity(_state.value.newAlbumTitleInput)
    }

    fun setNewAlbumNameValidity(valid: Boolean) = _state.update {
        it.copy(isInputNameValid = valid)
    }

    fun setIsAlbumCreatedSuccessfully(success: Boolean) = _state.update {
        it.copy(isAlbumCreatedSuccessfully = success)
    }

    private fun shouldAddAlbum(
        it: List<Photo>,
        key: Album,
    ) = it.isNotEmpty() || key == Album.FavouriteAlbum


    fun setCurrentAlbum(album: Album?) {
        _state.update {
            it.copy(currentAlbum = album)
        }
    }

    fun togglePhotoSelection(photo: Photo) {
        val selectedPhotos = _state.value.selectedPhotos.toMutableSet()
        if (photo in selectedPhotos) {
            selectedPhotos.remove(photo)
        } else {
            selectedPhotos.add(photo)
        }
        _state.update {
            it.copy(selectedPhotos = selectedPhotos)
        }
    }

    fun clearSelectedPhotos() {
        _state.update {
            it.copy(selectedPhotos = emptySet())
        }
    }

    fun selectAllPhotos() {
        _state.value.currentAlbum?.let { album ->
            val currentAlbumPhotos = _state.value.albums.getAlbumPhotos(album)
            val albumPhotos = when (_state.value.currentMediaType) {
                FilterMediaType.ALL_MEDIA -> currentAlbumPhotos
                FilterMediaType.IMAGES -> currentAlbumPhotos.filterIsInstance<Photo.Image>()
                FilterMediaType.VIDEOS -> currentAlbumPhotos.filterIsInstance<Photo.Video>()
            }
            _state.update {
                it.copy(selectedPhotos = albumPhotos.toMutableSet())
            }
        }
    }

    /**
     * Set the value for the viewstate property showRemovePhotosDialog
     */
    fun setShowRemovePhotosFromAlbumDialog(show: Boolean) {
        _state.update {
            it.copy(showRemovePhotosDialog = show)
        }
    }

    /**
     * Function to remove the currently selected photos from the current album
     */
    fun removePhotosFromAlbum() = viewModelScope.launch {
        (_state.value.currentAlbum as? Album.UserAlbum)?.let { album ->
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
    }

    fun getAlbumPhotosCount() =
        _state.value.albums.find { it.id == _state.value.currentAlbum }?.count ?: 0

    fun removeFavourites() {
        viewModelScope.launch {
            removeFavouritesUseCase(_state.value.selectedPhotos.map { it.id }.toList())
        }
        _state.update {
            it.copy(selectedPhotos = emptySet())
        }
    }

    suspend fun getSelectedNodes() =
        getNodeListByIds(_state.value.selectedPhotos.map { it.id }.toList())

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

    fun setSnackBarMessage(snackBarMessage: String) {
        _state.update {
            it.copy(snackBarMessage = snackBarMessage)
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

    fun setShowCreateAlbumDialog(showCreateDialog: Boolean) = _state.update {
        it.copy(showCreateAlbumDialog = showCreateDialog)
    }

    fun revalidateInput() {
        if (!_state.value.isInputNameValid && (_state.value.showCreateAlbumDialog || _state.value.showRenameDialog)) {
            checkLatestNameInputValidity()
        }
    }
}
