package mega.privacy.android.app.presentation.photos.albums.add

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.R
import mega.privacy.android.app.featuretoggle.ApiFeatures
import mega.privacy.android.app.presentation.extensions.getStorageState
import mega.privacy.android.app.presentation.videosection.mapper.VideoPlaylistUIEntityMapper
import mega.privacy.android.app.presentation.videosection.model.VideoPlaylistUIEntity
import mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.entity.photos.Album.UserAlbum
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.entity.videosection.UserVideoPlaylist
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.usecase.AddPhotosToAlbum
import mega.privacy.android.domain.usecase.GetAlbumPhotosUseCase
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.GetUserAlbums
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.photos.CreateAlbumUseCase
import mega.privacy.android.domain.usecase.photos.GetNextDefaultAlbumNameUseCase
import mega.privacy.android.domain.usecase.photos.GetProscribedAlbumNamesUseCase
import mega.privacy.android.domain.usecase.photos.GetTimelinePhotosUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.videosection.AddVideosToPlaylistUseCase
import mega.privacy.android.domain.usecase.videosection.CreateVideoPlaylistUseCase
import mega.privacy.android.domain.usecase.videosection.GetVideoPlaylistsUseCase
import mega.privacy.android.domain.usecase.videosection.MonitorVideoPlaylistSetsUpdateUseCase
import javax.inject.Inject

@HiltViewModel
internal class AddToAlbumViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getTimelinePhotosUseCase: GetTimelinePhotosUseCase,
    private val getUserAlbums: GetUserAlbums,
    private val getAlbumPhotos: GetAlbumPhotosUseCase,
    private val getNextDefaultAlbumNameUseCase: GetNextDefaultAlbumNameUseCase,
    private val getProscribedAlbumNamesUseCase: GetProscribedAlbumNamesUseCase,
    private val createAlbumUseCase: CreateAlbumUseCase,
    private val addPhotosToAlbum: AddPhotosToAlbum,
    private val getVideoPlaylistsUseCase: GetVideoPlaylistsUseCase,
    private val createVideoPlaylistUseCase: CreateVideoPlaylistUseCase,
    private val monitorVideoPlaylistSetsUpdateUseCase: MonitorVideoPlaylistSetsUpdateUseCase,
    private val addVideosToPlaylistUseCase: AddVideosToPlaylistUseCase,
    private val videoPlaylistUIEntityMapper: VideoPlaylistUIEntityMapper,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    @ApplicationScope private val appScope: CoroutineScope,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val state: MutableStateFlow<AddToAlbumState> = MutableStateFlow(AddToAlbumState())

    val stateFlow: StateFlow<AddToAlbumState> = state.asStateFlow()

    private val type: Int by lazy {
        savedStateHandle["type"] ?: 0
    }

    val nodeIds: List<NodeId> by lazy {
        savedStateHandle.get<Array<Long>?>("ids")?.map { NodeId(it) }.orEmpty()
    }

    private var isInitialized: Boolean = false

    private var isHiddenNodesVisible: Boolean = false

    private var createAlbumJob: Job? = null

    private var createPlaylistJob: Job? = null

    private var albumsMap: Map<AlbumId, List<Photo>> = mapOf()

    private var playlistsMap: Map<NodeId, List<TypedVideoNode>> = mapOf()

    private val showHiddenNodes: Boolean
        get() = isHiddenNodesVisible || state.value.accountType?.isPaid == false || state.value.isBusinessAccountExpired

    init {
        initialize()
    }

    private fun initialize() {
        state.update {
            it.copy(viewType = type)
        }

        viewModelScope.launch {
            if (getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)) {
                isHiddenNodesVisible = monitorShowHiddenItemsUseCase().firstOrNull() ?: false

                monitorAccountDetailUseCase()
                    .onEach(::handleAccountDetail)
                    .launchIn(viewModelScope)
            } else {
                isHiddenNodesVisible = true
            }

            getTimelinePhotosUseCase()
                .onEach { handlePhotos() }
                .launchIn(viewModelScope)

            if (type == 1) {
                loadVideoPlaylists()

                monitorVideoPlaylistSetsUpdateUseCase()
                    .conflate()
                    .onEach { loadVideoPlaylists() }
                    .launchIn(viewModelScope)
            }
        }
    }

    private suspend fun handleAccountDetail(accountDetail: AccountDetail) {
        val isBusinessAccountExpired = getBusinessStatusUseCase() == BusinessAccountStatus.Expired

        state.update {
            it.copy(
                accountType = accountDetail.levelDetail?.accountType,
                isBusinessAccountExpired = isBusinessAccountExpired,
            )
        }
    }

    private fun handlePhotos() {
        if (isInitialized) return
        isInitialized = true

        loadAlbums()
    }

    private fun loadAlbums() {
        getUserAlbums()
            .onEach(::handleAlbums)
            .launchIn(viewModelScope)
    }

    private suspend fun handleAlbums(albums: List<UserAlbum>) {
        val albumPhotos = albums.map { album ->
            val photos = getAlbumPhotos(
                albumId = album.id,
                refreshElements = true,
            ).firstOrNull().orEmpty()
            album to photos
        }
        albumsMap = albumPhotos.associate { (album, photos) ->
            album.id to photos
        }

        withContext(defaultDispatcher) {
            val albumsPhotos = albumPhotos.map { (album, photos) ->
                val cover = album.cover?.let { photo ->
                    photo.takeIf { showHiddenNodes || !it.isSensitive && !it.isSensitiveInherited }
                }

                val filteredPhotos =
                    photos.filter { showHiddenNodes || !it.isSensitive && !it.isSensitiveInherited }

                val updatedAlbum = album.copy(
                    cover = cover ?: filteredPhotos
                        .sortedWith((compareByDescending<Photo> { it.modificationTime }.thenByDescending { it.id }))
                        .firstOrNull(),
                )

                updatedAlbum to filteredPhotos.size
            }.sortedByDescending { it.first.creationTime }

            val selectedAlbum = state.value.selectedAlbum?.let { album ->
                albumsPhotos.firstOrNull { it.first.id == album.id }?.first
            }

            state.update {
                it.copy(
                    isLoadingAlbums = false,
                    albums = albumsPhotos,
                    selectedAlbum = selectedAlbum,
                )
            }
        }
    }

    fun selectAlbum(album: UserAlbum) {
        state.update {
            it.copy(selectedAlbum = album)
        }
    }

    fun setupNewAlbum(defaultAlbumName: String) {
        if (getStorageState() == StorageState.PayWall) {
            showOverDiskQuotaPaywallWarning()
            return
        }

        val albumNameSuggestion = getNextDefaultAlbumNameUseCase(
            defaultName = defaultAlbumName,
            currentNames = state.value.existingAlbumNames,
        )

        state.update {
            it.copy(
                isCreatingAlbum = true,
                albumNameSuggestion = albumNameSuggestion,
            )
        }
    }

    fun cancelAlbumCreation() {
        state.update {
            it.copy(isCreatingAlbum = false)
        }
    }

    fun clearAlbumNameErrorMessage() {
        state.update {
            it.copy(albumNameErrorMessageRes = null)
        }
    }

    fun createAlbum(albumName: String) {
        if (createAlbumJob?.isActive == true) return

        createAlbumJob = viewModelScope.launch {
            val errorMessageRes =
                if (getProscribedAlbumNamesUseCase().any { it.equals(albumName, true) }) {
                    R.string.photos_create_album_error_message_systems_album
                } else if (albumName in state.value.existingAlbumNames) {
                    R.string.photos_create_album_error_message_duplicate
                } else if ("[\\\\*/:<>?\"|]".toRegex().containsMatchIn(albumName)) {
                    R.string.invalid_characters_defined
                } else {
                    null
                }

            state.update {
                it.copy(albumNameErrorMessageRes = errorMessageRes)
            }

            if (errorMessageRes == null) {
                runCatching {
                    createAlbumUseCase(albumName)
                }.onSuccess {
                    cancelAlbumCreation()
                }
            }
        }
    }

    fun addPhotosToAlbum() = viewModelScope.launch {
        if (getStorageState() == StorageState.PayWall) {
            showOverDiskQuotaPaywallWarning()
            return@launch
        }

        val album = state.value.selectedAlbum ?: return@launch
        val photos = albumsMap[album.id].orEmpty()
        val nodeIds = nodeIds

        val (completionType, numAddedItems) = if (nodeIds.size == 1) {
            if (photos.any { it.id == nodeIds.firstOrNull()?.longValue }) {
                1 to 0
            } else {
                1 to nodeIds.size
            }
        } else if (nodeIds.all { nodeId -> photos.any { it.id == nodeId.longValue } }) {
            3 to 0
        } else if (nodeIds.none { nodeId -> photos.any { it.id == nodeId.longValue } }) {
            2 to nodeIds.size
        } else {
            2 to (nodeIds.map { it.longValue } - photos.map { it.id }).size
        }

        runCatching {
            addPhotosToAlbum(
                albumId = album.id,
                photoIds = nodeIds,
                isAsync = true,
            )
        }.onSuccess {
            state.update {
                it.copy(
                    mediaHolderName = album.title,
                    completionType = completionType,
                    numAddedItems = numAddedItems,
                    additionType = 0,
                )
            }
        }
    }

    private fun loadVideoPlaylists() = viewModelScope.launch {
        val playlists = getVideoPlaylistsUseCase().filterIsInstance<UserVideoPlaylist>()
        playlistsMap = playlists.associate {
            it.id to it.videos.orEmpty()
        }

        val uiPlaylists = playlists
            .map { playlist ->
                val filteredVideos = playlist.videos
                    .orEmpty()
                    .filter { showHiddenNodes || !it.isMarkedSensitive && !it.isSensitiveInherited }

                playlist.copy(videos = filteredVideos)
            }
            .map(videoPlaylistUIEntityMapper::invoke)

        val selectedPlaylist = state.value.selectedPlaylist?.let { playlist ->
            uiPlaylists.firstOrNull { it.id == playlist.id }
        }

        state.update {
            it.copy(
                isLoadingPlaylists = false,
                playlists = uiPlaylists,
                selectedPlaylist = selectedPlaylist,
            )
        }
    }

    fun selectPlaylist(playlist: VideoPlaylistUIEntity) {
        state.update {
            it.copy(selectedPlaylist = playlist)
        }
    }

    fun setupNewPlaylist(defaultPlaylistName: String) {
        if (getStorageState() == StorageState.PayWall) {
            showOverDiskQuotaPaywallWarning()
            return
        }

        val playlistNameSuggestion = getNextDefaultAlbumNameUseCase(
            defaultName = defaultPlaylistName,
            currentNames = state.value.existingPlaylistNames,
        )

        state.update {
            it.copy(
                isCreatingPlaylist = true,
                playlistNameSuggestion = playlistNameSuggestion,
            )
        }
    }

    fun cancelPlaylistCreation() {
        state.update {
            it.copy(isCreatingPlaylist = false)
        }
    }

    fun clearPlaylistNameErrorMessage() {
        state.update {
            it.copy(playlistNameErrorMessageRes = null)
        }
    }

    fun createPlaylist(playlistName: String) {
        if (createPlaylistJob?.isActive == true) return

        createPlaylistJob = viewModelScope.launch {
            val errorMessageRes =
                if (playlistName.isBlank()) {
                    R.string.invalid_string
                } else if (playlistName in state.value.existingPlaylistNames) {
                    -888
                } else if ("[\\\\*/:<>?\"|]".toRegex().containsMatchIn(playlistName)) {
                    R.string.invalid_characters_defined
                } else {
                    null
                }

            state.update {
                it.copy(playlistNameErrorMessageRes = errorMessageRes)
            }

            if (errorMessageRes == null) {
                runCatching {
                    createVideoPlaylistUseCase(playlistName)
                }.onSuccess {
                    cancelPlaylistCreation()
                }
            }
        }
    }

    fun addVideosToPlaylist() {
        if (getStorageState() == StorageState.PayWall) {
            showOverDiskQuotaPaywallWarning()
            return
        }

        val playlist = state.value.selectedPlaylist ?: return
        val videos = playlistsMap[playlist.id].orEmpty()
        val nodeIds = nodeIds

        val (completionType, numAddedItems) = if (nodeIds.size == 1) {
            1 to 1
        } else if (nodeIds.all { nodeId -> videos.any { it.id == nodeId } }) {
            2 to 0
        } else {
            2 to nodeIds.size
        }

        appScope.launch {
            runCatching {
                addVideosToPlaylistUseCase(
                    playlistID = playlist.id,
                    videoIDs = nodeIds,
                )
            }
        }

        state.update {
            it.copy(
                mediaHolderName = playlist.title,
                completionType = completionType,
                numAddedItems = numAddedItems,
                additionType = 1,
            )
        }
    }
}
