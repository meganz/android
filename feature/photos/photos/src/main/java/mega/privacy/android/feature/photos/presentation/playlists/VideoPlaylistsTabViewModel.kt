package mega.privacy.android.feature.photos.presentation.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.core.nodecomponents.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.node.sort.MonitorSortCloudOrderUseCase
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
    private val videoPlaylistUiEntityMapper: VideoPlaylistUiEntityMapper,
    private val setCloudSortOrderUseCase: SetCloudSortOrder,
    private val nodeSortConfigurationUiMapper: NodeSortConfigurationUiMapper,
    monitorSortCloudOrderUseCase: MonitorSortCloudOrderUseCase,
) : ViewModel() {
    private val triggerFlow = MutableStateFlow(false)

    internal val uiState: StateFlow<VideoPlaylistsTabUiState> by lazy {
        triggerFlow.flatMapLatest {
            combine(
                monitorVideoPlaylistSetsUpdateUseCase()
                    .onStart { emit(emptyList()) },
                monitorNodeUpdatesUseCase()
                    .filter {
                        it.changes.keys.any { node ->
                            node is FileNode && node.type is VideoFileTypeInfo
                        }
                    }.onStart { emit(NodeUpdate(emptyMap())) },
                monitorSortCloudOrderUseCase(),
            ) { _, _, sortOrder ->
                val videoPlaylists = getVideoPlaylistsUseCase()
                val videoPlaylistEntities = videoPlaylists.map {
                    videoPlaylistUiEntityMapper(it)
                }

                val convertedSortOrder =
                    sortOrder?.convertPlaylistSortOrder() ?: SortOrder.ORDER_DEFAULT_ASC
                val sortOrderPair = nodeSortConfigurationUiMapper(convertedSortOrder)

                VideoPlaylistsTabUiState.Data(
                    videoPlaylists = videoPlaylists,
                    videoPlaylistEntities = videoPlaylistEntities,
                    sortOrder = convertedSortOrder,
                    selectedSortConfiguration = sortOrderPair,
                )
            }.catch {
                Timber.e(it)
            }
        }.asUiStateFlow(
            viewModelScope,
            VideoPlaylistsTabUiState.Loading
        )
    }

    private fun SortOrder.convertPlaylistSortOrder() =
        when (this) {
            SortOrder.ORDER_DEFAULT_DESC -> SortOrder.ORDER_DEFAULT_DESC
            SortOrder.ORDER_CREATION_ASC -> SortOrder.ORDER_CREATION_ASC
            SortOrder.ORDER_CREATION_DESC -> SortOrder.ORDER_CREATION_DESC
            SortOrder.ORDER_MODIFICATION_ASC -> SortOrder.ORDER_MODIFICATION_ASC
            SortOrder.ORDER_MODIFICATION_DESC -> SortOrder.ORDER_MODIFICATION_DESC
            else -> SortOrder.ORDER_DEFAULT_ASC
        }

    fun triggerRefresh() {
        triggerFlow.update { !it }
    }

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