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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
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
import mega.privacy.android.domain.usecase.videosection.GetVideoPlaylistByIdUseCase
import mega.privacy.android.domain.usecase.videosection.MonitorVideoPlaylistSetsUpdateUseCase
import mega.privacy.android.domain.usecase.videosection.UpdateVideoPlaylistTitleUseCase
import mega.privacy.android.feature.photos.mapper.VideoPlaylistDetailUiEntityMapper
import mega.privacy.android.feature.photos.mapper.VideoPlaylistTitleValidationErrorMessageMapper
import mega.privacy.android.feature.photos.presentation.playlists.VideoPlaylistEditState
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
    @Assisted private val playlistHandle: Long,
    @Assisted private val type: PlaylistType,
) : ViewModel() {
    internal val videoPlaylistEditState: StateFlow<VideoPlaylistEditState>
        field: MutableStateFlow<VideoPlaylistEditState> = MutableStateFlow(VideoPlaylistEditState())

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

    @AssistedFactory
    interface Factory {
        fun create(playlistHandle: Long, type: PlaylistType): VideoPlaylistDetailViewModel
    }
}