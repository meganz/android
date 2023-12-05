package mega.privacy.android.app.presentation.videosection

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.app.domain.usecase.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.app.presentation.videosection.mapper.UIVideoMapper
import mega.privacy.android.app.presentation.videosection.model.UIVideo
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
import mega.privacy.android.domain.usecase.videosection.GetAllVideosUseCase
import nz.mega.sdk.MegaNode
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

    init {
        viewModelScope.launch {
            merge(
                monitorNodeUpdatesUseCase(),
                monitorOfflineNodeUpdatesUseCase()
            ).collectLatest {
                setPendingRefreshNodes()
            }
        }
    }

    private fun setPendingRefreshNodes() = _state.update { it.copy(isPendingRefresh = true) }

    internal fun refreshNodes() = viewModelScope.launch {
        _state.update {
            it.copy(
                allVideos = getUIVideoList().updateOriginalData().filterVideosBySearchQuery(),
                sortOrder = getCloudSortOrder(),
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

    internal fun onTabSelected(selectTab: VideoSectionTab) = _tabState.update {
        it.copy(selectedTab = selectTab)
    }

    internal fun refreshWhenOrderChanged() =
        viewModelScope.launch {
            _state.update {
                it.copy(
                    sortOrder = getCloudSortOrder(),
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
        searchNodeByQueryString()
    }

    internal fun exitSearch() {
        _state.update { it.copy(searchMode = false) }
        searchQuery = ""
        refreshNodes()
    }

    private fun searchNodeByQueryString() {
        _state.update {
            it.copy(
                allVideos = originalData.filter { video ->
                    video.name.contains(searchQuery, true)
                },
                scrollToTop = true
            )
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
        _state.update {
            it.copy(
                allVideos = clearVideosSelected(),
                selectedVideoHandles = emptyList(),
                isInSelection = false
            )
        }
    }

    private fun clearVideosSelected() = _state.value.allVideos.map {
        it.copy(isSelected = false)
    }

    internal fun selectAllNodes() {
        _state.update {
            it.copy(
                allVideos = _state.value.allVideos.map { item ->
                    item.copy(isSelected = true)
                },
                selectedVideoHandles = _state.value.allVideos.map { item ->
                    item.id.longValue
                },
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
        _state.update {
            it.copy(
                allVideos = _state.value.allVideos.updateItemSelectedState(index, isSelected),
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
}
