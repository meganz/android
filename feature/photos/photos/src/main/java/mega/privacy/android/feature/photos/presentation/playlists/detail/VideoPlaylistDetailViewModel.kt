package mega.privacy.android.feature.photos.presentation.playlists.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.videosection.PlaylistType
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.videosection.GetVideoPlaylistByIdUseCase
import mega.privacy.android.domain.usecase.videosection.MonitorVideoPlaylistSetsUpdateUseCase
import mega.privacy.android.feature.photos.mapper.VideoPlaylistDetailUiEntityMapper
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel(assistedFactory = VideoPlaylistDetailViewModel.Factory::class)
class VideoPlaylistDetailViewModel @AssistedInject constructor(
    private val videoPlaylistDetailUiEntityMapper: VideoPlaylistDetailUiEntityMapper,
    private val getVideoPlaylistByIdUseCase: GetVideoPlaylistByIdUseCase,
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    private val monitorVideoPlaylistSetsUpdateUseCase: MonitorVideoPlaylistSetsUpdateUseCase,
    @Assisted private val playlistHandle: Long,
    @Assisted private val type: PlaylistType,
) : ViewModel() {

    internal val uiState: StateFlow<VideoPlaylistDetailUiState> by lazy(LazyThreadSafetyMode.NONE) {
        combine(
            monitorVideoPlaylistSetsUpdateUseCase()
                .filter {
                    playlistHandle in it
                }.onStart { emit(emptyList()) },
            monitorNodeUpdatesUseCase()
                .filter { nodeUpdate ->
                    isMatchRefreshCondition(nodeUpdate)
                }.onStart { emit(NodeUpdate(emptyMap())) },
        ) {
            val videoPlaylist = getVideoPlaylistByIdUseCase(NodeId(playlistHandle), type)
            val videoPlaylistUiEntity = videoPlaylist?.let {
                videoPlaylistDetailUiEntityMapper(it)
            }

            VideoPlaylistDetailUiState.Data(
                playlistDetail = videoPlaylistUiEntity,
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

    @AssistedFactory
    interface Factory {
        fun create(playlistHandle: Long, type: PlaylistType): VideoPlaylistDetailViewModel
    }
}