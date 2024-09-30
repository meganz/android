package mega.privacy.android.app.presentation.videosection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.R
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.node.FileNodeContent
import mega.privacy.android.app.presentation.videosection.mapper.VideoPlaylistUIEntityMapper
import mega.privacy.android.app.presentation.videosection.mapper.VideoUIEntityMapper
import mega.privacy.android.app.presentation.videosection.model.DurationFilterOption
import mega.privacy.android.app.presentation.videosection.model.LocationFilterOption
import mega.privacy.android.app.presentation.videosection.model.VideoPlaylistUIEntity
import mega.privacy.android.app.presentation.videosection.model.VideoSectionState
import mega.privacy.android.app.presentation.videosection.model.VideoSectionTab
import mega.privacy.android.app.presentation.videosection.model.VideoSectionTabState
import mega.privacy.android.app.presentation.videosection.model.VideoUIEntity
import mega.privacy.android.app.presentation.videosection.view.playlist.videoPlaylistDetailRoute
import mega.privacy.android.app.presentation.videosection.view.videoSectionRoute
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.entity.videosection.VideoPlaylist
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.IsHiddenNodesOnboardedUseCase
import mega.privacy.android.domain.usecase.UpdateNodeSensitiveUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.node.GetNodeContentUriUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.photos.GetNextDefaultAlbumNameUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.videosection.AddVideosToPlaylistUseCase
import mega.privacy.android.domain.usecase.videosection.ClearRecentlyWatchedVideosUseCase
import mega.privacy.android.domain.usecase.videosection.CreateVideoPlaylistUseCase
import mega.privacy.android.domain.usecase.videosection.GetAllVideosUseCase
import mega.privacy.android.domain.usecase.videosection.GetSyncUploadsFolderIdsUseCase
import mega.privacy.android.domain.usecase.videosection.GetVideoPlaylistsUseCase
import mega.privacy.android.domain.usecase.videosection.GetVideoRecentlyWatchedUseCase
import mega.privacy.android.domain.usecase.videosection.MonitorVideoPlaylistSetsUpdateUseCase
import mega.privacy.android.domain.usecase.videosection.RemoveRecentlyWatchedItemUseCase
import mega.privacy.android.domain.usecase.videosection.RemoveVideoPlaylistsUseCase
import mega.privacy.android.domain.usecase.videosection.RemoveVideosFromPlaylistUseCase
import mega.privacy.android.domain.usecase.videosection.UpdateVideoPlaylistTitleUseCase
import mega.privacy.android.legacy.core.ui.model.SearchWidgetState
import nz.mega.sdk.MegaNode
import timber.log.Timber
import javax.inject.Inject

/**
 * Videos section view model
 */
@HiltViewModel
class VideoSectionViewModel @Inject constructor(
    private val getAllVideosUseCase: GetAllVideosUseCase,
    private val videoUIEntityMapper: VideoUIEntityMapper,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    private val monitorOfflineNodeUpdatesUseCase: MonitorOfflineNodeUpdatesUseCase,
    private val getNodeByHandle: GetNodeByHandle,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val getVideoPlaylistsUseCase: GetVideoPlaylistsUseCase,
    private val videoPlaylistUIEntityMapper: VideoPlaylistUIEntityMapper,
    private val createVideoPlaylistUseCase: CreateVideoPlaylistUseCase,
    private val addVideosToPlaylistUseCase: AddVideosToPlaylistUseCase,
    private val getNextDefaultAlbumNameUseCase: GetNextDefaultAlbumNameUseCase,
    private val removeVideoPlaylistsUseCase: RemoveVideoPlaylistsUseCase,
    private val updateVideoPlaylistTitleUseCase: UpdateVideoPlaylistTitleUseCase,
    private val getSyncUploadsFolderIdsUseCase: GetSyncUploadsFolderIdsUseCase,
    private val removeVideosFromPlaylistUseCase: RemoveVideosFromPlaylistUseCase,
    private val monitorVideoPlaylistSetsUpdateUseCase: MonitorVideoPlaylistSetsUpdateUseCase,
    private val updateNodeSensitiveUseCase: UpdateNodeSensitiveUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val isHiddenNodesOnboardedUseCase: IsHiddenNodesOnboardedUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val getNodeContentUriUseCase: GetNodeContentUriUseCase,
    private val getVideoRecentlyWatchedUseCase: GetVideoRecentlyWatchedUseCase,
    private val clearRecentlyWatchedVideosUseCase: ClearRecentlyWatchedVideosUseCase,
    private val removeRecentlyWatchedItemUseCase: RemoveRecentlyWatchedItemUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(VideoSectionState())

    /**
     * The state regarding the business logic
     */
    val state: StateFlow<VideoSectionState> = _state.asStateFlow()

    private val _tabState = MutableStateFlow(VideoSectionTabState())

    private var showHiddenItems: Boolean? = null

    /**
     * The state regarding the tabs
     */
    val tabState = _tabState.asStateFlow()

    private val originalData = mutableListOf<TypedVideoNode>()
    private val originalEntities = mutableListOf<VideoUIEntity>()

    private val originalPlaylistData = mutableListOf<VideoPlaylist>()
    private val originalPlaylistEntities = mutableListOf<VideoPlaylistUIEntity>()

    private var createVideoPlaylistJob: Job? = null
    private var searchQueryJob: Job? = null

    init {
        refreshNodesIfAnyUpdates()
        viewModelScope.launch {
            monitorVideoPlaylistSetsUpdateUseCase().conflate()
                .catch {
                    Timber.e(it)
                }.collect {
                    if (it.isNotEmpty()) {
                        if (_state.value.currentVideoPlaylist != null) {
                            refreshVideoPlaylistsWithUpdateCurrentVideoPlaylist()
                        } else {
                            loadVideoPlaylists()
                        }
                    }
                }
        }

        viewModelScope.launch {
            if (getFeatureFlagValueUseCase(AppFeatures.HiddenNodes)) {
                handleHiddenNodesUIFlow()
                monitorIsHiddenNodesOnboarded()
            }
        }

        viewModelScope.launch {
            _state.map { it.selectedVideoHandles }.distinctUntilChanged().collectLatest {
                if (it.isNotEmpty()) {
                    checkActionsVisible()
                }
            }
        }

        viewModelScope.launch {
            _state.map { it.selectedVideoElementIDs }.distinctUntilChanged().collectLatest {
                if (it.isNotEmpty()) {
                    checkPlaylistDetailActionsVisible()
                }
            }
        }
    }

    private fun handleHiddenNodesUIFlow() {
        combine(
            monitorAccountDetailUseCase(),
            monitorShowHiddenItemsUseCase(),
        ) { accountDetail, showHiddenItems ->
            this@VideoSectionViewModel.showHiddenItems = showHiddenItems
            _state.update {
                it.copy(
                    accountDetail = accountDetail,
                    isPendingRefresh = true
                )
            }
        }.catch { Timber.e(it) }
            .launchIn(viewModelScope)
    }

    private fun refreshNodesIfAnyUpdates() {
        viewModelScope.launch {
            merge(
                monitorNodeUpdatesUseCase().filter {
                    it.changes.keys.any { node ->
                        node is FileNode && node.type is VideoFileTypeInfo
                    }
                },
                monitorOfflineNodeUpdatesUseCase()
            ).conflate()
                .catch {
                    Timber.e(it)
                }.collect {
                    setPendingRefreshNodes()
                }
        }
    }

    private fun loadVideoPlaylists() {
        viewModelScope.launch(defaultDispatcher) {
            val videoPlaylists =
                getVideoPlaylists().updateOriginalPlaylistEntities()
                    .filterVideoPlaylistsBySearchQuery()
            _state.update {
                it.copy(
                    videoPlaylists = videoPlaylists,
                    isPlaylistProgressBarShown = false,
                    scrollToTop = false
                )
            }
        }
    }

    private suspend fun getVideoPlaylists() =
        getVideoPlaylistsUseCase().updateOriginalPlaylistData().map { videoPlaylist ->
            videoPlaylistUIEntityMapper(videoPlaylist)
        }

    private fun List<VideoPlaylist>.updateOriginalPlaylistData() = also { data ->
        if (originalPlaylistData.isNotEmpty()) {
            originalPlaylistData.clear()
        }
        originalPlaylistData.addAll(data)
    }

    private fun List<VideoPlaylistUIEntity>.updateOriginalPlaylistEntities() = also { data ->
        if (originalPlaylistEntities.isNotEmpty()) {
            originalPlaylistEntities.clear()
        }
        originalPlaylistEntities.addAll(data)
    }

    private fun List<VideoPlaylistUIEntity>.filterVideoPlaylistsBySearchQuery() =
        filter { playlist ->
            playlist.title.contains(_state.value.query ?: "", true)
        }

    private fun setPendingRefreshNodes() = _state.update { it.copy(isPendingRefresh = true) }

    internal fun refreshNodes() = viewModelScope.launch(defaultDispatcher) {
        val videoList = filterNonSensitiveItems(
            items = getVideoUIEntityList(),
            showHiddenItems = this@VideoSectionViewModel.showHiddenItems,
            isPaid = _state.value.accountDetail?.levelDetail?.accountType?.isPaid,
        ).filterVideosByDuration()
            .filterVideosByLocation()
            .updateOriginalEntities()
            .filterVideosBySearchQuery()

        val sortOrder = getCloudSortOrder()
        _state.update {
            it.copy(
                allVideos = videoList,
                sortOrder = sortOrder,
                progressBarShowing = false,
                scrollToTop = false
            )
        }
    }

    private suspend fun getVideoUIEntityList() =
        getAllVideosUseCase().updateOriginalData().map { videoUIEntityMapper(it) }

    private fun List<TypedVideoNode>.updateOriginalData() = also { data ->
        if (originalData.isNotEmpty()) {
            originalData.clear()
        }
        originalData.addAll(data)
    }

    private fun List<VideoUIEntity>.filterVideosBySearchQuery() =
        filter { video ->
            video.name.contains(_state.value.query ?: "", true)
        }

    private fun List<VideoUIEntity>.updateOriginalEntities() = also { data ->
        if (originalEntities.isNotEmpty()) {
            originalEntities.clear()
        }
        originalEntities.addAll(data)
    }

    private suspend fun List<VideoUIEntity>.filterVideosByLocation(): List<VideoUIEntity> {
        val syncUploadsFolderIds = getSyncUploadsFolderIdsUseCase()
        return filter {
            when (_state.value.locationSelectedFilterOption) {
                LocationFilterOption.AllLocations -> true

                LocationFilterOption.CloudDrive ->
                    it.parentId.longValue !in syncUploadsFolderIds

                LocationFilterOption.CameraUploads ->
                    it.parentId.longValue in syncUploadsFolderIds

                LocationFilterOption.SharedItems -> it.isSharedItems
            }
        }
    }


    private fun List<VideoUIEntity>.filterVideosByDuration() =
        filter {
            val seconds = it.duration.inWholeSeconds
            when (_state.value.durationSelectedFilterOption) {
                DurationFilterOption.AllDurations -> true

                DurationFilterOption.LessThan10Seconds -> seconds < 10

                DurationFilterOption.Between10And60Seconds -> seconds in 10..60

                DurationFilterOption.Between1And4 -> seconds in 60..240

                DurationFilterOption.Between4And20 -> seconds in 241..1200

                DurationFilterOption.MoreThan20 -> seconds > 1200
            }
        }

    internal fun markHandledPendingRefresh() = _state.update { it.copy(isPendingRefresh = false) }

    internal fun onTabSelected(selectTab: VideoSectionTab) {
        if (selectTab == VideoSectionTab.Playlists && originalPlaylistEntities.isEmpty()) {
            loadVideoPlaylists()
        }
        if (_state.value.searchState == SearchWidgetState.EXPANDED) {
            exitSearch()
        }
        if (_state.value.isInSelection) {
            clearAllSelectedVideos()
            clearAllSelectedVideoPlaylists()
        }
        _tabState.update {
            it.copy(selectedTab = selectTab)
        }
    }

    internal fun refreshWhenOrderChanged() =
        viewModelScope.launch {
            val sortOrder = getCloudSortOrder()
            _state.update {
                it.copy(
                    sortOrder = sortOrder,
                    progressBarShowing = true,
                    isPlaylistProgressBarShown = true
                )
            }
            setPendingRefreshNodes()
            loadVideoPlaylists()
        }

    internal fun shouldShowSearchMenu() =
        _state.value.currentVideoPlaylist == null && _state.value.allVideos.isNotEmpty()

    internal fun searchQuery(queryString: String) {
        searchQueryJob?.cancel()
        searchQueryJob = viewModelScope.launch {
            delay(200)
            _state.update {
                it.copy(
                    query = queryString
                )
            }

            if (_tabState.value.selectedTab == VideoSectionTab.All) {
                searchNodeByQueryString()
            } else {
                searchPlaylistByQueryString()
            }
        }
    }

    private fun searchNodeByQueryString() {
        val videos = originalEntities.filter { video ->
            video.name.contains(_state.value.query ?: "", true)
        }
        _state.update {
            it.copy(
                allVideos = videos,
                scrollToTop = true
            )
        }
    }

    private fun searchPlaylistByQueryString() {
        val playlists = originalPlaylistEntities.filter { playlist ->
            playlist.title.contains(_state.value.query ?: "", true)
        }
        _state.update {
            it.copy(
                videoPlaylists = playlists,
                scrollToTop = true
            )
        }
    }

    internal fun exitSearch() {
        _state.update {
            it.copy(
                query = null,
                searchState = SearchWidgetState.COLLAPSED
            )
        }

        if (_tabState.value.selectedTab == VideoSectionTab.All) {
            refreshNodes()
        } else {
            loadVideoPlaylists()
        }
    }

    internal fun searchWidgetStateUpdate() {
        val searchState = when (_state.value.searchState) {
            SearchWidgetState.EXPANDED -> SearchWidgetState.COLLAPSED

            SearchWidgetState.COLLAPSED -> SearchWidgetState.EXPANDED
        }
        _state.update { it.copy(searchState = searchState) }
    }

    internal fun clearAllSelectedVideos() {
        val videos = clearVideosSelected()
        _state.update {
            it.copy(
                allVideos = videos,
                selectedVideoHandles = emptyList(),
                isInSelection = false
            )
        }
    }

    private fun clearVideosSelected() = _state.value.allVideos.map {
        it.copy(isSelected = false)
    }

    internal fun clearAllSelectedVideoPlaylists() {
        val playlists = clearVideoPlaylistsSelected()
        _state.update {
            it.copy(
                videoPlaylists = playlists,
                selectedVideoPlaylistHandles = emptyList(),
                isInSelection = false
            )
        }
    }

    internal fun clearAllSelectedVideosOfPlaylist() {
        _state.value.currentVideoPlaylist?.let { playlist ->
            val updatedVideos = clearVideosSelectedOfPlaylist(playlist) ?: return@let
            val updatedPlaylist = playlist.copy(videos = updatedVideos)
            _state.update {
                it.copy(
                    currentVideoPlaylist = updatedPlaylist,
                    selectedVideoElementIDs = emptyList(),
                    isInSelection = false
                )
            }
        }
    }

    private fun clearVideoPlaylistsSelected() = _state.value.videoPlaylists.map {
        it.copy(isSelected = false)
    }

    private fun clearVideosSelectedOfPlaylist(playlist: VideoPlaylistUIEntity) =
        playlist.videos?.map { it.copy(isSelected = false) }

    internal fun selectAllNodes() {
        val videos = _state.value.allVideos.map { item ->
            item.copy(isSelected = true)
        }
        val selectedHandles = _state.value.allVideos.map { item ->
            item.id.longValue
        }
        _state.update {
            it.copy(
                allVideos = videos,
                selectedVideoHandles = selectedHandles,
                isInSelection = true
            )
        }
    }

    internal fun selectAllVideoPlaylists() {
        val playlists = _state.value.videoPlaylists.map { item ->
            item.copy(isSelected = true)
        }
        val selectedHandles = _state.value.videoPlaylists.map { item ->
            item.id.longValue
        }
        _state.update {
            it.copy(
                videoPlaylists = playlists,
                selectedVideoPlaylistHandles = selectedHandles,
                isInSelection = true
            )
        }
    }

    internal fun selectAllVideosOfPlaylist() =
        _state.value.currentVideoPlaylist?.let { playlist ->
            if (playlist.videos == null) return@let

            val selectedHandles = playlist.videos.mapNotNull { it.elementID }

            if (playlist.videos.size == selectedHandles.size) {
                val videos = playlist.videos.map { item ->
                    item.copy(isSelected = true)
                }
                val updatedCurrentVideoPlaylist = playlist.copy(videos = videos)
                _state.update {
                    it.copy(
                        currentVideoPlaylist = updatedCurrentVideoPlaylist,
                        selectedVideoElementIDs = selectedHandles,
                        isInSelection = true
                    )
                }
            }

        }

    internal fun onItemClicked(item: VideoUIEntity, index: Int) {
        if (_state.value.isInSelection) {
            updateVideoItemInSelectionState(item = item, index = index)
        } else {
            updateClickedItem(getTypedVideoNodeById(item.id))
        }
    }

    internal fun onItemLongClicked(item: VideoUIEntity, index: Int) =
        updateVideoItemInSelectionState(item = item, index = index)

    private fun updateVideoItemInSelectionState(item: VideoUIEntity, index: Int) {
        val isSelected = !item.isSelected
        val selectedHandles = updateSelectedHandles(
            videoID = item.id.longValue,
            isSelected = isSelected,
            selectedHandles = _state.value.selectedVideoHandles
        )
        val videos = _state.value.allVideos.updateItemSelectedState(index, isSelected)
        _state.update {
            it.copy(
                allVideos = videos,
                selectedVideoHandles = selectedHandles,
                isInSelection = selectedHandles.isNotEmpty()
            )
        }
    }

    private fun List<VideoUIEntity>.updateItemSelectedState(index: Int, isSelected: Boolean) =
        if (index in indices) {
            toMutableList().also { list ->
                list[index] = list[index].copy(isSelected = isSelected)
            }
        } else this


    private fun updateSelectedHandles(
        videoID: Long,
        isSelected: Boolean,
        selectedHandles: List<Long>,
    ) =
        selectedHandles.toMutableList().also { handles ->
            if (isSelected) {
                handles.add(videoID)
            } else {
                handles.remove(videoID)
            }
        }

    internal fun onVideoPlaylistItemClicked(item: VideoPlaylistUIEntity, index: Int) =
        updateVideoPlaylistItemInSelectionState(item = item, index = index)

    private fun updateVideoPlaylistItemInSelectionState(item: VideoPlaylistUIEntity, index: Int) {
        val isSelected = !item.isSelected
        val selectedHandles = updateSelectedHandles(
            videoID = item.id.longValue,
            isSelected = isSelected,
            selectedHandles = _state.value.selectedVideoPlaylistHandles
        )
        val updatedPlaylists =
            _state.value.videoPlaylists.updateVideoPlaylistItemSelectedState(index, isSelected)
        _state.update {
            it.copy(
                videoPlaylists = updatedPlaylists,
                selectedVideoPlaylistHandles = selectedHandles,
                isInSelection = selectedHandles.isNotEmpty()
            )
        }
    }

    private fun List<VideoPlaylistUIEntity>.updateVideoPlaylistItemSelectedState(
        index: Int,
        isSelected: Boolean,
    ) =
        if (index in indices) {
            toMutableList().also { list ->
                list[index] = list[index].copy(isSelected = isSelected)
            }
        } else this

    internal fun onVideoItemOfPlaylistLongClicked(item: VideoUIEntity, index: Int) =
        updateVideoItemOfPlaylistInSelectionState(item = item, index = index)

    internal fun onVideoItemOfPlaylistClicked(item: VideoUIEntity, index: Int) {
        if (state.value.isInSelection) {
            updateVideoItemOfPlaylistInSelectionState(item = item, index = index)
        } else {
            updateClickedPlaylistDetailItem(getTypedVideoNodeOfPlaylistById(item.id))
        }
    }

    private fun updateVideoItemOfPlaylistInSelectionState(item: VideoUIEntity, index: Int) =
        _state.value.currentVideoPlaylist?.let { playlist ->
            if (playlist.videos == null || item.elementID == null) return@let
            val isSelected = !item.isSelected
            val updatedVideos =
                playlist.videos.updateItemSelectedState(index, isSelected)
            val selectedHandles = updateSelectedHandles(
                videoID = item.elementID,
                isSelected = isSelected,
                selectedHandles = _state.value.selectedVideoElementIDs
            )
            val updatedCurrentPlaylist = playlist.copy(videos = updatedVideos)
            _state.update {
                it.copy(
                    currentVideoPlaylist = updatedCurrentPlaylist,
                    selectedVideoElementIDs = selectedHandles,
                    isInSelection = selectedHandles.isNotEmpty()
                )
            }
        }

    internal suspend fun getSelectedNodes(): List<TypedNode> {
        return _state.value.currentDestinationRoute?.let { route ->
            when (route) {
                videoSectionRoute -> {
                    _state.value.selectedVideoHandles.mapNotNull {
                        runCatching {
                            getNodeByIdUseCase(NodeId(it))
                        }.getOrNull()
                    }
                }

                videoPlaylistDetailRoute -> {
                    _state.value.currentVideoPlaylist?.let { playlist ->
                        val selectedVideoElementIDs = _state.value.selectedVideoElementIDs
                        val videos = playlist.videos ?: emptyList()
                        val selectedVideos = selectedVideoElementIDs.mapNotNull { elementId ->
                            videos.find { video -> video.elementID == elementId }
                        }
                        selectedVideos.mapNotNull { video ->
                            runCatching {
                                getNodeByIdUseCase(video.id)
                            }.getOrNull()
                        }
                    }
                }

                else -> emptyList()
            }
        } ?: emptyList()
    }

    internal suspend fun getSelectedMegaNode(): List<MegaNode> =
        _state.value.selectedVideoHandles.mapNotNull {
            runCatching {
                getNodeByHandle(it)
            }.getOrNull()
        }

    /**
     * Create new video playlist
     *
     * @param title video playlist title
     */
    internal fun createNewPlaylist(title: String) {
        if (createVideoPlaylistJob?.isActive == true) return
        title.ifEmpty {
            _state.value.createVideoPlaylistPlaceholderTitle
        }.trim()
            .takeIf { it.isNotEmpty() && checkVideoPlaylistTitleValidity(it) }
            ?.let { playlistTitle ->
                createVideoPlaylistJob = viewModelScope.launch {
                    runCatching {
                        createVideoPlaylistUseCase(playlistTitle)
                    }.onSuccess { videoPlaylist ->
                        _state.update {
                            it.copy(
                                currentVideoPlaylist = videoPlaylistUIEntityMapper(
                                    videoPlaylist
                                ),
                                isVideoPlaylistCreatedSuccessfully = true
                            )
                        }
                        loadVideoPlaylists()
                        Timber.d("Current video playlist: ${videoPlaylist.title}")
                    }.onFailure { exception ->
                        Timber.e(exception)
                        _state.update {
                            it.copy(isVideoPlaylistCreatedSuccessfully = false)
                        }
                    }
                }
            }
    }

    internal fun removeVideoPlaylists(deletedList: List<VideoPlaylistUIEntity>) =
        viewModelScope.launch {
            runCatching {
                removeVideoPlaylistsUseCase(deletedList.map { it.id })
            }.onSuccess { deletedPlaylistIDs ->
                val deletedPlaylistTitles =
                    getDeletedVideoPlaylistTitles(
                        playlistIDs = deletedPlaylistIDs,
                        deletedPlaylist = deletedList
                    )
                Timber.d("removeVideoPlaylists deletedPlaylistTitles: $deletedPlaylistTitles")
                _state.update {
                    it.copy(
                        deletedVideoPlaylistTitles = deletedPlaylistTitles,
                        areVideoPlaylistsRemovedSuccessfully = true
                    )
                }
                loadVideoPlaylists()
            }.onFailure { exception ->
                Timber.e(exception)
                _state.update {
                    it.copy(
                        areVideoPlaylistsRemovedSuccessfully = false
                    )
                }
            }
        }

    private fun getDeletedVideoPlaylistTitles(
        playlistIDs: List<Long>,
        deletedPlaylist: List<VideoPlaylistUIEntity>,
    ): List<String> = playlistIDs.mapNotNull { id ->
        deletedPlaylist.firstOrNull { it.id.longValue == id }?.title
    }

    /**
     * Add videos to the playlist
     *
     * @param playlistID playlist id
     * @param videoIDs added video ids
     */
    internal fun addVideosToPlaylist(playlistID: NodeId, videoIDs: List<NodeId>) =
        viewModelScope.launch {
            runCatching {
                addVideosToPlaylistUseCase(playlistID, videoIDs)
            }.onSuccess { numberOfAddedVideos ->
                _state.update {
                    it.copy(
                        numberOfAddedVideos = numberOfAddedVideos
                    )
                }
                refreshVideoPlaylistsWithUpdateCurrentVideoPlaylist()
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }

    private fun refreshVideoPlaylistsWithUpdateCurrentVideoPlaylist() =
        viewModelScope.launch {
            val videoPlaylists =
                getVideoPlaylists().updateOriginalPlaylistEntities()
                    .filterVideoPlaylistsBySearchQuery()
            val updatedCurrentVideoPlaylist = videoPlaylists.firstOrNull {
                it.id == _state.value.currentVideoPlaylist?.id
            }
            _state.update {
                it.copy(
                    videoPlaylists = videoPlaylists,
                    isPlaylistProgressBarShown = false,
                    scrollToTop = false,
                    currentVideoPlaylist = updatedCurrentVideoPlaylist
                )
            }
        }

    internal fun removeVideosFromPlaylist(playlistID: NodeId, videoElementIDs: List<Long>) =
        viewModelScope.launch {
            runCatching {
                removeVideosFromPlaylistUseCase(playlistID, videoElementIDs)
            }.onSuccess { numberOfRemovedItems ->
                _state.update {
                    it.copy(
                        numberOfRemovedItems = numberOfRemovedItems
                    )
                }
                refreshVideoPlaylistsWithUpdateCurrentVideoPlaylist()
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }

    internal fun updateVideoPlaylistTitle(playlistID: NodeId, newTitle: String) =
        newTitle.trim()
            .takeIf { it.isNotEmpty() && checkVideoPlaylistTitleValidity(it) }
            ?.let { title ->
                viewModelScope.launch {
                    runCatching {
                        updateVideoPlaylistTitleUseCase(playlistID, title)
                    }.onSuccess { title ->
                        Timber.d("Updated video playlist title: $title")
                        refreshVideoPlaylistsWithUpdatedTitle(title)
                    }.onFailure { exception ->
                        Timber.e(exception)
                    }
                }
            }

    private fun refreshVideoPlaylistsWithUpdatedTitle(newTitle: String) =
        viewModelScope.launch {
            val videoPlaylists =
                getVideoPlaylists().updateOriginalPlaylistEntities()
                    .filterVideoPlaylistsBySearchQuery()
            val updatedCurrentVideoPlaylist =
                _state.value.currentVideoPlaylist?.copy(title = newTitle)
            _state.update {
                it.copy(
                    videoPlaylists = videoPlaylists,
                    isPlaylistProgressBarShown = false,
                    scrollToTop = false,
                    currentVideoPlaylist = updatedCurrentVideoPlaylist
                )
            }
        }

    internal fun updateCurrentVideoPlaylist(playlist: VideoPlaylistUIEntity?) {
        _state.update {
            it.copy(currentVideoPlaylist = playlist)
        }
    }

    internal fun setPlaceholderTitle(placeholderTitle: String) {
        val playlistTitles = getAllVideoPlaylistTitles()
        _state.update {
            it.copy(
                createVideoPlaylistPlaceholderTitle = getNextDefaultAlbumNameUseCase(
                    defaultName = placeholderTitle,
                    currentNames = playlistTitles
                )
            )
        }
    }

    private fun getAllVideoPlaylistTitles() = _state.value.videoPlaylists.map { it.title }

    internal fun setNewPlaylistTitleValidity(valid: Boolean) = _state.update {
        it.copy(isInputTitleValid = valid)
    }

    private fun checkVideoPlaylistTitleValidity(
        title: String,
    ): Boolean {
        var errorMessage: Int? = null
        var isTitleValid = true

        if (title.isBlank()) {
            isTitleValid = false
            errorMessage = R.string.invalid_string
        } else if (title in getAllVideoPlaylistTitles()) {
            isTitleValid = false
            errorMessage = ERROR_MESSAGE_REPEATED_TITLE
        } else if ("[\\\\*/:<>?\"|]".toRegex().containsMatchIn(title)) {
            isTitleValid = false
            errorMessage = R.string.invalid_characters_defined
        }

        _state.update {
            it.copy(
                isInputTitleValid = isTitleValid,
                createDialogErrorMessage = errorMessage
            )
        }

        return isTitleValid
    }

    internal fun setIsVideoPlaylistCreatedSuccessfully(value: Boolean) = _state.update {
        it.copy(isVideoPlaylistCreatedSuccessfully = value)
    }

    internal fun clearDeletedVideoPlaylistTitles() = _state.update {
        it.copy(deletedVideoPlaylistTitles = emptyList())
    }

    internal fun setAreVideoPlaylistsRemovedSuccessfully(value: Boolean) = _state.update {
        it.copy(areVideoPlaylistsRemovedSuccessfully = value)
    }

    internal fun setCurrentDestinationRoute(route: String?) = _state.update {
        it.copy(currentDestinationRoute = route)
    }

    internal fun setLocationSelectedFilterOption(locationFilterOption: LocationFilterOption) =
        _state.update {
            it.copy(
                locationSelectedFilterOption = locationFilterOption,
                progressBarShowing = true,
                isPendingRefresh = true
            )
        }

    internal fun setDurationSelectedFilterOption(durationFilterOption: DurationFilterOption) =
        _state.update {
            it.copy(
                durationSelectedFilterOption = durationFilterOption,
                progressBarShowing = true,
                isPendingRefresh = true
            )
        }

    internal fun setUpdateToolbarTitle(value: String?) = _state.update {
        it.copy(updateToolbarTitle = value)
    }

    internal fun clearNumberOfAddedVideos() = _state.update { it.copy(numberOfAddedVideos = 0) }

    internal fun clearNumberOfRemovedItems() = _state.update { it.copy(numberOfRemovedItems = 0) }

    internal fun hideOrUnhideNodes(nodeIds: List<NodeId>, hide: Boolean) = viewModelScope.launch {
        for (nodeId in nodeIds) {
            async {
                runCatching {
                    updateNodeSensitiveUseCase(nodeId = nodeId, isSensitive = hide)
                }.onFailure { Timber.e("Update sensitivity failed: $it") }
            }
        }
    }

    internal suspend fun unhideNodes() {
        hideOrUnhideNodes(
            nodeIds = getSelectedNodes()
                .map { it.id },
            hide = false,
        )
    }

    private fun monitorAccountDetail() {
        monitorAccountDetailUseCase()
            .onEach { accountDetail ->
                _state.update {
                    it.copy(accountDetail = accountDetail)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun monitorIsHiddenNodesOnboarded() {
        viewModelScope.launch {
            val isHiddenNodesOnboarded = isHiddenNodesOnboardedUseCase()
            _state.update {
                it.copy(isHiddenNodesOnboarded = isHiddenNodesOnboarded)
            }
        }
    }

    /**
     * Mark hidden nodes onboarding has shown
     */
    fun setHiddenNodesOnboarded() {
        _state.update {
            it.copy(isHiddenNodesOnboarded = true)
        }
    }

    private suspend fun filterNonSensitiveItems(
        items: List<VideoUIEntity>,
        showHiddenItems: Boolean?,
        isPaid: Boolean?,
    ) = withContext(defaultDispatcher) {
        showHiddenItems ?: return@withContext items
        isPaid ?: return@withContext items

        return@withContext if (showHiddenItems || !isPaid) {
            items
        } else {
            items.filter { !it.isMarkedSensitive && !it.isSensitiveInherited }
        }
    }

    internal suspend fun getNodeContentUri(fileNode: TypedFileNode) =
        FileNodeContent.AudioOrVideo(uri = getNodeContentUriUseCase(fileNode)).uri

    internal fun getTypedVideoNodeById(id: NodeId) = originalData.firstOrNull { it.id == id }

    internal fun getTypedVideoNodeOfPlaylistById(id: NodeId) =
        originalPlaylistData.firstOrNull {
            it.id == _state.value.currentVideoPlaylist?.id
        }?.let { playlist ->
            playlist.videos?.firstOrNull { it.id == id }
        }

    internal fun updateClickedItem(value: TypedVideoNode?) =
        _state.update { it.copy(clickedItem = value) }

    internal fun updateClickedPlaylistDetailItem(value: TypedVideoNode?) =
        _state.update { it.copy(clickedPlaylistDetailItem = value) }

    internal fun playAllButtonClicked() {
        val clickedItem = _state.value.currentVideoPlaylist?.videos?.firstOrNull() ?: return
        updateClickedPlaylistDetailItem(getTypedVideoNodeOfPlaylistById(clickedItem.id))
    }

    internal suspend fun checkActionsVisible() {
        var isHideMenuActionVisible = false
        var isUnhideMenuActionVisible = false
        var isRemoveLinkMenuActionVisible = false

        if (_tabState.value.selectedTab == VideoSectionTab.All) {
            val selectedNodes = getSelectedNodes()
            val isHiddenNodesEnabled = getFeatureFlagValueUseCase(AppFeatures.HiddenNodes)
            val includeSensitiveInheritedNode = selectedNodes.any { it.isSensitiveInherited }

            if (isHiddenNodesEnabled) {
                val hasNonSensitiveNode =
                    getSelectedNodes().any { !it.isMarkedSensitive }
                val isPaid =
                    _state.value.accountDetail?.levelDetail?.accountType?.isPaid ?: false
                isHideMenuActionVisible =
                    !isPaid || (hasNonSensitiveNode && !includeSensitiveInheritedNode)
                isUnhideMenuActionVisible =
                    isPaid && !hasNonSensitiveNode && !includeSensitiveInheritedNode
            }

            isRemoveLinkMenuActionVisible = if (selectedNodes.size == 1) {
                selectedNodes.firstOrNull()?.let {
                    it.exportedData != null
                } ?: false
            } else {
                false
            }
        }

        _state.update {
            it.copy(
                isHideMenuActionVisible = isHideMenuActionVisible,
                isUnhideMenuActionVisible = isUnhideMenuActionVisible,
                isRemoveLinkMenuActionVisible = isRemoveLinkMenuActionVisible
            )
        }
    }

    private suspend fun checkPlaylistDetailActionsVisible() {
        var isHideMenuActionVisible = false
        var isUnhideMenuActionVisible = false
        _state.value.currentVideoPlaylist?.let { playlist ->
            val selectedVideoElementIDs = _state.value.selectedVideoElementIDs
            val videos = playlist.videos ?: return
            val selectedVideos = selectedVideoElementIDs.mapNotNull { elementId ->
                videos.find { video -> video.elementID == elementId }
            }
            val isHiddenNodesEnabled = getFeatureFlagValueUseCase(AppFeatures.HiddenNodes)
            val includeSensitiveInheritedNode = selectedVideos.any { it.isSensitiveInherited }

            if (isHiddenNodesEnabled) {
                val hasNonSensitiveNode =
                    selectedVideos.any { !it.isMarkedSensitive }
                val isPaid =
                    _state.value.accountDetail?.levelDetail?.accountType?.isPaid ?: false
                isHideMenuActionVisible =
                    !isPaid || (hasNonSensitiveNode && !includeSensitiveInheritedNode)
                isUnhideMenuActionVisible =
                    isPaid && !hasNonSensitiveNode && !includeSensitiveInheritedNode
            }
        }
        _state.update {
            it.copy(
                isHideMenuActionVisible = isHideMenuActionVisible,
                isUnhideMenuActionVisible = isUnhideMenuActionVisible,
            )
        }
    }

    internal fun loadRecentlyWatchedVideos() = viewModelScope.launch {
        runCatching {
            getVideoRecentlyWatchedUseCase().map {
                videoUIEntityMapper(it)
            }.groupBy { it.watchedDate }
        }.onSuccess { group ->
            _state.update {
                it.copy(groupedVideoRecentlyWatchedItems = group)
            }
        }.onFailure { error ->
            Timber.e(error)
            _state.update {
                it.copy(groupedVideoRecentlyWatchedItems = emptyMap())
            }
        }
    }

    internal suspend fun isRecentlyWatchedEnabled() =
        getFeatureFlagValueUseCase(AppFeatures.VideoRecentlyWatched)

    internal fun clearRecentlyWatchedVideos() {
        viewModelScope.launch {
            runCatching {
                clearRecentlyWatchedVideosUseCase()
                _state.update {
                    it.copy(
                        groupedVideoRecentlyWatchedItems = emptyMap(),
                        clearRecentlyWatchedVideosSuccess = triggered
                    )
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    internal fun removeRecentlyWatchedItem(handle: Long) {
        viewModelScope.launch {
            runCatching {
                removeRecentlyWatchedItemUseCase(handle)
                _state.update {
                    it.copy(
                        removeRecentlyWatchedItemSuccess = triggered
                    )
                }
                loadRecentlyWatchedVideos()
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    internal fun resetClearRecentlyWatchedVideosSuccess() =
        _state.update {
            it.copy(clearRecentlyWatchedVideosSuccess = consumed)
        }

    internal fun resetRemoveRecentlyWatchedItemSuccess() =
        _state.update {
            it.copy(removeRecentlyWatchedItemSuccess = consumed)
        }

    companion object {
        private const val ERROR_MESSAGE_REPEATED_TITLE = 0
    }
}
