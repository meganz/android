package mega.privacy.android.feature.photos.presentation.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.videosection.GetVideoPlaylistsUseCase
import mega.privacy.android.domain.usecase.videosection.MonitorVideoPlaylistSetsUpdateUseCase
import mega.privacy.android.feature.photos.mapper.VideoPlaylistUiEntityMapper
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import timber.log.Timber
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class VideoPlaylistsTabViewModel @Inject constructor(
    private val getVideoPlaylistsUseCase: GetVideoPlaylistsUseCase,
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    private val monitorVideoPlaylistSetsUpdateUseCase: MonitorVideoPlaylistSetsUpdateUseCase,
    private val videoPlaylistUiEntityMapper: VideoPlaylistUiEntityMapper
) : ViewModel() {
    private val triggerFlow = MutableStateFlow(false)

    internal val uiState: StateFlow<VideoPlaylistsTabUiState> by lazy {
        triggerFlow.flatMapLatest {
            merge(
                monitorVideoPlaylistSetsUpdateUseCase(),
                monitorNodeUpdatesUseCase().filter {
                    it.changes.keys.any { node ->
                        node is FileNode && node.type is VideoFileTypeInfo
                    }
                }
            ).mapLatest {
                val videoPlaylists = getVideoPlaylistsUseCase()
                val videoPlaylistEntities = videoPlaylists.map {
                    videoPlaylistUiEntityMapper(it)
                }

                VideoPlaylistsTabUiState.Data(
                    videoPlaylists = videoPlaylists,
                    videoPlaylistEntities = videoPlaylistEntities
                )
            }.catch {
                Timber.e(it)
            }
        }.asUiStateFlow(
            viewModelScope,
            VideoPlaylistsTabUiState.Loading
        )
    }

    fun triggerRefresh() {
        triggerFlow.update { !it }
    }
}