package mega.privacy.android.feature.photos.presentation.videos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
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
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.videosection.GetAllVideosUseCase
import mega.privacy.android.feature.photos.mapper.VideoUiEntityMapper
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class VideosTabViewModel @Inject constructor(
    private val getAllVideosUseCase: GetAllVideosUseCase,
    private val videoUiEntityMapper: VideoUiEntityMapper,
    private val getCloudSortOrder: GetCloudSortOrder,
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    private val monitorOfflineNodeUpdatesUseCase: MonitorOfflineNodeUpdatesUseCase,
) : ViewModel() {
    private val triggerFlow = MutableStateFlow(false)
    private val queryFlow = MutableStateFlow<String?>(null)
    internal val uiState: StateFlow<VideosTabUiState> by lazy {
        triggerFlow.flatMapLatest {
            merge(
                monitorNodeUpdatesUseCase().filter {
                    it.changes.keys.any { node ->
                        node is FileNode && node.type is VideoFileTypeInfo
                    }
                },
                monitorOfflineNodeUpdatesUseCase()
            ).mapLatest {
                val videoList = getVideoUIEntityList()
                val sortOrder = getCloudSortOrder()

                VideosTabUiState.Data(
                    allVideos = videoList,
                    sortOrder = sortOrder,
                    query = queryFlow.value
                )
            }.catch {
                Timber.e(it)
            }
        }.asUiStateFlow(
            viewModelScope,
            VideosTabUiState.Loading
        )
    }

    fun triggerRefresh() {
        triggerFlow.update { !it }
    }

    private suspend fun getVideoUIEntityList() =
        getAllVideosUseCase().map { videoUiEntityMapper(it) }
}