package mega.privacy.android.feature.photos.presentation.playlists.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.videosection.PlaylistType
import mega.privacy.android.domain.exception.account.PlaylistNameValidationException
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.videosection.GetVideoPlaylistByIdUseCase
import mega.privacy.android.domain.usecase.videosection.MonitorVideoPlaylistSetsUpdateUseCase
import mega.privacy.android.domain.usecase.videosection.RemoveVideoPlaylistsUseCase
import mega.privacy.android.domain.usecase.videosection.UpdateVideoPlaylistTitleUseCase
import mega.privacy.android.feature.photos.mapper.VideoPlaylistDetailUiEntityMapper
import mega.privacy.android.feature.photos.mapper.VideoPlaylistTitleValidationErrorMessageMapper
import mega.privacy.android.feature.photos.presentation.playlists.VideoPlaylistEditState
import mega.privacy.android.feature.photos.presentation.playlists.model.VideoPlaylistUiEntity
import mega.privacy.android.feature.photos.presentation.videos.model.VideoUiEntity
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = VideoPlaylistDetailViewModel.Factory::class)
class VideoPlaylistDetailViewModel @AssistedInject constructor(
    private val videoPlaylistDetailUiEntityMapper: VideoPlaylistDetailUiEntityMapper,
    private val getVideoPlaylistByIdUseCase: GetVideoPlaylistByIdUseCase,
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    private val monitorVideoPlaylistSetsUpdateUseCase: MonitorVideoPlaylistSetsUpdateUseCase,
    private val videoPlaylistTitleValidationErrorMessageMapper: VideoPlaylistTitleValidationErrorMessageMapper,
    private val updateVideoPlaylistTitleUseCase: UpdateVideoPlaylistTitleUseCase,
    private val removeVideoPlaylistsUseCase: RemoveVideoPlaylistsUseCase,
    private val monitorHiddenNodesEnabledUseCase: MonitorHiddenNodesEnabledUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    @Assisted private val playlistHandle: Long,
    @Assisted private val type: PlaylistType,
) : ViewModel() {
    private val selectedVideoIdsFlow = MutableStateFlow<Set<Long>>(emptySet())

    internal val videoPlaylistEditState: StateFlow<VideoPlaylistEditState>
        field: MutableStateFlow<VideoPlaylistEditState> = MutableStateFlow(VideoPlaylistEditState())

    private fun combinedTriggerFlow(): Flow<Unit> = merge(
        monitorVideoPlaylistSetsUpdateUseCase()
            .filter {
                playlistHandle in it
            }.mapLatest { }
            .catch { Timber.e(it) },
        monitorNodeUpdatesUseCase()
            .filter { nodeUpdate ->
                isMatchRefreshCondition(nodeUpdate)
            }.mapLatest { }
            .catch { Timber.e(it) }
    ).onStart { emit(Unit) }

    private fun getVideoPlaylist() =
        combinedTriggerFlow().mapLatest {
            runCatching {
                getVideoPlaylistByIdUseCase(NodeId(playlistHandle), type)
            }.getOrNull()
        }

    internal val uiState: StateFlow<VideoPlaylistDetailUiState> by lazy(LazyThreadSafetyMode.NONE) {
        combine(
            getVideoPlaylist(),
            monitorHiddenNodesEnabledUseCase().catch { Timber.e(it) },
            monitorShowHiddenItemsUseCase().catch { Timber.e(it) },
            selectedVideoIdsFlow
        ) { videoPlaylist, isHiddenNodesEnabled, isShowHiddenItems, selectedVideoIds ->
            val showHiddenItems = isShowHiddenItems || !isHiddenNodesEnabled
            val videoPlaylistUiEntity = videoPlaylist?.let {
                videoPlaylistDetailUiEntityMapper(it, showHiddenItems, selectedVideoIds)
            }

            VideoPlaylistDetailUiState.Data(
                playlistDetail = videoPlaylistUiEntity,
                showHiddenItems = showHiddenItems,
                selectedTypedNodes = videoPlaylist?.videos?.filter {
                    it.id.longValue in selectedVideoIds
                }?.toSet() ?: emptySet(),
                isHiddenNodesEnabled = isHiddenNodesEnabled
            )
        }.catch {
            Timber.e(it)
        }.asUiStateFlow(
            viewModelScope,
            VideoPlaylistDetailUiState.Loading
        )
    }

    private fun isMatchRefreshCondition(nodeUpdate: NodeUpdate): Boolean {
        val isVideoNode = nodeUpdate.changes.keys.any { node ->
            node is FileNode && node.type is VideoFileTypeInfo
        }
        val changedNodeIds = nodeUpdate.changes.keys.map { change -> change.id.longValue }
        return isVideoNode && hasMatchingIdWithPlaylist(changedNodeIds)
    }

    private fun hasMatchingIdWithPlaylist(list: List<Long>): Boolean {
        if (uiState.value !is VideoPlaylistDetailUiState.Data) return false
        val state = uiState.value as VideoPlaylistDetailUiState.Data
        val ids = state.playlistDetail?.videos?.map { it.id.longValue }?.toSet() ?: emptySet()
        return list.any { id -> id in ids }
    }

    internal fun updateVideoPlaylistTitle(playlistID: NodeId, newTitle: String) {
        viewModelScope.launch {
            val title = newTitle.trim()
            runCatching {
                updateVideoPlaylistTitleUseCase(playlistID, title)
            }.onSuccess { title ->
                Timber.d("Updated video playlist title: $title")
                videoPlaylistEditState.update { it.copy(updateTitleSuccessEvent = triggered) }
            }.onFailure { exception ->
                Timber.e(exception)
                if (exception is PlaylistNameValidationException) {
                    val errorMessage = videoPlaylistTitleValidationErrorMessageMapper(exception)
                    videoPlaylistEditState.update {
                        it.copy(
                            editVideoPlaylistErrorMessage = errorMessage
                        )
                    }
                }
            }
        }
    }

    internal fun resetUpdateTitleSuccessEvent() {
        videoPlaylistEditState.update {
            it.copy(
                updateTitleSuccessEvent = consumed
            )
        }
    }

    internal fun resetEditVideoPlaylistErrorMessage() {
        videoPlaylistEditState.update {
            it.copy(
                editVideoPlaylistErrorMessage = null
            )
        }
    }

    internal fun showUpdateVideoPlaylistDialog() {
        videoPlaylistEditState.update {
            it.copy(
                showUpdateVideoPlaylistDialog = true
            )
        }
    }

    internal fun resetUpdateVideoPlaylistDialogEvent() {
        videoPlaylistEditState.update {
            it.copy(
                showUpdateVideoPlaylistDialog = false
            )
        }
    }

    internal fun removeVideoPlaylists(deletedItems: Set<VideoPlaylistUiEntity>) =
        viewModelScope.launch {
            runCatching {
                val ids = deletedItems.map { it.id }
                removeVideoPlaylistsUseCase(ids)
            }.onSuccess { deletedPlaylistIDs ->
                val deletedTitlesById = deletedItems.associate {
                    it.id.longValue to it.title
                }
                val deletedPlaylistTitles = deletedPlaylistIDs.mapNotNull { id ->
                    deletedTitlesById[id]
                }
                Timber.d("removeVideoPlaylists deletedPlaylistTitles: $deletedPlaylistTitles")
                videoPlaylistEditState.update {
                    it.copy(
                        playlistsRemovedEvent = triggered(deletedPlaylistTitles)
                    )
                }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }

    internal fun resetPlaylistsRemovedEvent() {
        videoPlaylistEditState.update {
            it.copy(
                playlistsRemovedEvent = consumed()
            )
        }
    }

    internal fun onItemClicked(item: VideoUiEntity) {
        if (selectedVideoIdsFlow.value.isNotEmpty()) {
            toggleItemSelection(item)
        }
    }

    internal fun onItemLongClicked(item: VideoUiEntity) {
        toggleItemSelection(item = item)
    }

    private fun toggleItemSelection(item: VideoUiEntity) {
        val updatedSelectedIds = selectedVideoIdsFlow.value.toMutableSet()
        if (item.id.longValue in updatedSelectedIds) {
            updatedSelectedIds -= item.id.longValue
        } else {
            updatedSelectedIds += item.id.longValue
        }
        selectedVideoIdsFlow.update { updatedSelectedIds }
    }

    internal fun selectAllVideos() {
        val state = uiState.value as? VideoPlaylistDetailUiState.Data ?: return
        val allIds = state.playlistDetail?.videos?.map { it.id.longValue }?.toSet() ?: return
        selectedVideoIdsFlow.update { allIds }
    }

    internal fun clearSelection() {
        selectedVideoIdsFlow.update { emptySet() }
    }

    @AssistedFactory
    interface Factory {
        fun create(playlistHandle: Long, type: PlaylistType): VideoPlaylistDetailViewModel
    }
}