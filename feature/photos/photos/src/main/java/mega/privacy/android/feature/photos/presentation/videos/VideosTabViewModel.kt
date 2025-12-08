package mega.privacy.android.feature.photos.presentation.videos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.core.nodecomponents.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.node.sort.MonitorSortCloudOrderUseCase
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
    private val setCloudSortOrderUseCase: SetCloudSortOrder,
    private val nodeSortConfigurationUiMapper: NodeSortConfigurationUiMapper,
    private val monitorSortCloudOrderUseCase: MonitorSortCloudOrderUseCase,
) : ViewModel() {
    private val triggerFlow = MutableStateFlow(false)
    private val queryFlow = MutableStateFlow<String?>(null)
    internal val uiState: StateFlow<VideosTabUiState> by lazy {
        triggerFlow.flatMapLatest {
            flow {
                emit(VideosTabUiState.Loading)
                emitAll(
                    merge(
                        monitorNodeUpdatesUseCase().filter {
                            it.changes.keys.any { node ->
                                node is FileNode && node.type is VideoFileTypeInfo
                            }
                        },
                        monitorOfflineNodeUpdatesUseCase(),
                        monitorSortCloudOrderUseCase()
                    ).mapLatest {
                        val videoList = getVideoUIEntityList()
                        val sortOrder = getCloudSortOrder()
                        val sortOrderPair = nodeSortConfigurationUiMapper(sortOrder)

                        VideosTabUiState.Data(
                            allVideos = videoList,
                            sortOrder = sortOrder,
                            query = queryFlow.value,
                            selectedSortConfiguration = sortOrderPair
                        )
                    }.catch {
                        Timber.e(it)
                    }
                )
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

    internal fun setCloudSortOrder(sortConfiguration: NodeSortConfiguration) {
        viewModelScope.launch {
            runCatching {
                val order = nodeSortConfigurationUiMapper(sortConfiguration)
                setCloudSortOrderUseCase(order)
            }.onFailure {
                Timber.e(it, "Failed to set cloud sort order")
            }
        }
    }
}