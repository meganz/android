package mega.privacy.android.app.presentation.videosection

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.presentation.videosection.mapper.UIVideoMapper
import mega.privacy.android.app.presentation.videosection.mapper.UIVideoPlaylistMapper
import mega.privacy.android.app.presentation.videosection.model.UIVideo
import mega.privacy.android.app.presentation.videosection.model.UIVideoPlaylist
import mega.privacy.android.app.presentation.videosection.model.VideoSectionState
import mega.privacy.android.app.presentation.videosection.model.VideoSectionTab
import mega.privacy.android.app.presentation.videosection.model.VideoSectionTabState
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER
import mega.privacy.android.app.utils.FileUtil.getDownloadLocation
import mega.privacy.android.app.utils.FileUtil.getLocalFile
import mega.privacy.android.app.utils.FileUtil.isFileAvailable
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetFileUrlByNodeHandleUseCase
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.file.GetFingerprintUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.videosection.AddVideosToPlaylistUseCase
import mega.privacy.android.domain.usecase.videosection.CreateVideoPlaylistUseCase
import mega.privacy.android.domain.usecase.videosection.GetAllVideosUseCase
import mega.privacy.android.domain.usecase.videosection.GetVideoPlaylistsUseCase
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Videos section view model
 */
@HiltViewModel
class VideoSectionViewModel @Inject constructor(
    private val getAllVideosUseCase: GetAllVideosUseCase,
    private val uiVideoMapper: UIVideoMapper,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    private val monitorOfflineNodeUpdatesUseCase: MonitorOfflineNodeUpdatesUseCase,
    private val getNodeByHandle: GetNodeByHandle,
    private val getFingerprintUseCase: GetFingerprintUseCase,
    private val megaApiHttpServerIsRunningUseCase: MegaApiHttpServerIsRunningUseCase,
    private val megaApiHttpServerStartUseCase: MegaApiHttpServerStartUseCase,
    private val getFileUrlByNodeHandleUseCase: GetFileUrlByNodeHandleUseCase,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val getVideoPlaylistsUseCase: GetVideoPlaylistsUseCase,
    private val uiVideoPlaylistMapper: UIVideoPlaylistMapper,
    private val createVideoPlaylistUseCase: CreateVideoPlaylistUseCase,
    private val addVideosToPlaylistUseCase: AddVideosToPlaylistUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(VideoSectionState())

    /**
     * The state regarding the business logic
     */
    val state: StateFlow<VideoSectionState> = _state.asStateFlow()

    private val _tabState = MutableStateFlow(VideoSectionTabState())

    /**
     * The state regarding the tabs
     */
    val tabState = _tabState.asStateFlow()

    private var searchQuery = ""
    private val originalData = mutableListOf<UIVideo>()
    private val originalPlaylistData = mutableListOf<UIVideoPlaylist>()

    private var createVideoPlaylistJob: Job? = null

    init {
        refreshNodesIfAnyUpdates()
    }

    private fun refreshNodesIfAnyUpdates() {
        viewModelScope.launch {
            merge(
                monitorNodeUpdatesUseCase(),
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
        viewModelScope.launch {
            val videoPlaylists =
                getVideoPlaylists().updateOriginalPlaylistData().filterVideoPlaylistsBySearchQuery()
            _state.update {
                it.copy(
                    videoPlaylists = videoPlaylists,
                    isPlaylistProgressBarShown = false,
                    scrollToTop = false
                )
            }
        }
    }

    private suspend fun getVideoPlaylists() = getVideoPlaylistsUseCase().map { videoPlaylist ->
        uiVideoPlaylistMapper(videoPlaylist)
    }

    private fun List<UIVideoPlaylist>.updateOriginalPlaylistData() = also { data ->
        if (originalPlaylistData.isNotEmpty()) {
            originalPlaylistData.clear()
        }
        originalPlaylistData.addAll(data)
    }

    private fun List<UIVideoPlaylist>.filterVideoPlaylistsBySearchQuery() =
        filter { playlist ->
            playlist.title.contains(searchQuery, true)
        }

    private fun setPendingRefreshNodes() = _state.update { it.copy(isPendingRefresh = true) }

    internal fun refreshNodes() = viewModelScope.launch {
        val videoList = getUIVideoList().updateOriginalData().filterVideosBySearchQuery()
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

    private fun List<UIVideo>.filterVideosBySearchQuery() =
        filter { video ->
            video.name.contains(searchQuery, true)
        }

    private fun List<UIVideo>.updateOriginalData() = also { data ->
        if (originalData.isNotEmpty()) {
            originalData.clear()
        }
        originalData.addAll(data)
    }

    private suspend fun getUIVideoList() = getAllVideosUseCase().map { uiVideoMapper(it) }

    internal fun markHandledPendingRefresh() = _state.update { it.copy(isPendingRefresh = false) }

    internal fun onTabSelected(selectTab: VideoSectionTab) {
        if (selectTab == VideoSectionTab.Playlists && originalPlaylistData.isEmpty()) {
            loadVideoPlaylists()
        }
        if (_state.value.searchMode) {
            exitSearch()
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
                    progressBarShowing = true
                )
            }
            setPendingRefreshNodes()
        }

    internal fun shouldShowSearchMenu() = _state.value.allVideos.isNotEmpty()

    internal fun searchReady() {
        if (_state.value.searchMode)
            return

        _state.update { it.copy(searchMode = true) }
        searchQuery = ""
    }

    internal fun searchQuery(query: String) {
        if (searchQuery == query)
            return

        searchQuery = query

        if (_tabState.value.selectedTab == VideoSectionTab.All) {
            searchNodeByQueryString()
        } else {
            searchPlaylistByQueryString()
        }
    }

    private fun searchNodeByQueryString() {
        val videos = originalData.filter { video ->
            video.name.contains(searchQuery, true)
        }
        _state.update {
            it.copy(
                allVideos = videos,
                scrollToTop = true
            )
        }
    }

    private fun searchPlaylistByQueryString() {
        val playlists = originalPlaylistData.filter { playlist ->
            playlist.title.contains(searchQuery, true)
        }
        _state.update {
            it.copy(
                videoPlaylists = playlists,
                scrollToTop = true
            )
        }
    }

    internal fun exitSearch() {
        _state.update { it.copy(searchMode = false) }
        searchQuery = ""

        if (_tabState.value.selectedTab == VideoSectionTab.All) {
            refreshNodes()
        } else {
            loadVideoPlaylists()
        }
    }

    /**
     * Detect the node whether is local file
     *
     * @param handle node handle
     * @return true is local file, otherwise is false
     */
    internal suspend fun isLocalFile(
        handle: Long,
    ): String? =
        getNodeByHandle(handle)?.let { node ->
            val localPath = getLocalFile(node)
            File(getDownloadLocation(), node.name).let { file ->
                if (localPath != null && ((isFileAvailable(file) && file.length() == node.size)
                            || (node.fingerprint == getFingerprintUseCase(localPath)))
                ) {
                    localPath
                } else {
                    null
                }
            }
        }

    /**
     * Update intent
     *
     * @param handle node handle
     * @param name node name
     * @param intent Intent
     * @return updated intent
     */
    internal suspend fun updateIntent(
        handle: Long,
        name: String,
        intent: Intent,
    ): Intent {
        if (megaApiHttpServerIsRunningUseCase() == 0) {
            megaApiHttpServerStartUseCase()
            intent.putExtra(INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, true)
        }

        getFileUrlByNodeHandleUseCase(handle)?.let { url ->
            Uri.parse(url)?.let { uri ->
                intent.setDataAndType(uri, MimeTypeList.typeForName(name).type)
            }
        }

        return intent
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

    internal fun onItemClicked(item: UIVideo, index: Int) {
        updateVideoItemInSelectionState(item = item, index = index)
    }

    internal fun onLongItemClicked(item: UIVideo, index: Int) =
        updateVideoItemInSelectionState(item = item, index = index)

    private fun updateVideoItemInSelectionState(item: UIVideo, index: Int) {
        val isSelected = !item.isSelected
        val selectedHandles = updateSelectedVideoHandles(item, isSelected)
        val videos = _state.value.allVideos.updateItemSelectedState(index, isSelected)
        _state.update {
            it.copy(
                allVideos = videos,
                selectedVideoHandles = selectedHandles,
                isInSelection = selectedHandles.isNotEmpty()
            )
        }
    }

    private fun List<UIVideo>.updateItemSelectedState(index: Int, isSelected: Boolean) =
        if (index in indices) {
            toMutableList().also { list ->
                list[index] = list[index].copy(isSelected = isSelected)
            }
        } else this


    private fun updateSelectedVideoHandles(item: UIVideo, isSelected: Boolean) =
        _state.value.selectedVideoHandles.toMutableList().also { selectedHandles ->
            if (isSelected) {
                selectedHandles.add(item.id.longValue)
            } else {
                selectedHandles.remove(item.id.longValue)
            }
        }

    internal suspend fun getSelectedNodes(): List<TypedNode> =
        _state.value.selectedVideoHandles.mapNotNull {
            runCatching {
                getNodeByIdUseCase(NodeId(it))
            }.getOrNull()
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
        createVideoPlaylistJob = viewModelScope.launch {
            runCatching {
                title.trim().takeIf { it.isNotEmpty() }?.let { playlistTitle ->
                    createVideoPlaylistUseCase(playlistTitle)
                }
            }.onSuccess { videoPlaylist ->
                _state.update {
                    it.copy(
                        currentVideoPlaylist = videoPlaylist,
                        isVideoPlaylistCreatedSuccessfully = true
                    )
                }
                Timber.d("Current video playlist: ${videoPlaylist?.title}")
            }.onFailure { exception ->
                Timber.e(exception)
                _state.update {
                    it.copy(isVideoPlaylistCreatedSuccessfully = false)
                }
            }
        }
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
            }
        }
}
