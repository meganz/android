package mega.privacy.android.feature.photos.presentation.videos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.videosection.ClearRecentlyWatchedVideosUseCase
import mega.privacy.android.domain.usecase.videosection.MonitorVideoRecentlyWatchedUseCase
import mega.privacy.android.feature.photos.mapper.VideoUiEntityMapper
import mega.privacy.android.feature.photos.presentation.videos.model.VideoUiEntity
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class VideoRecentlyWatchedViewModel @Inject constructor(
    private val monitorVideoRecentlyWatchedUseCase: MonitorVideoRecentlyWatchedUseCase,
    private val monitorOfflineNodeUpdatesUseCase: MonitorOfflineNodeUpdatesUseCase,
    private val monitorHiddenNodesEnabledUseCase: MonitorHiddenNodesEnabledUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
    private val videoUiEntityMapper: VideoUiEntityMapper,
    private val clearRecentlyWatchedVideosUseCase: ClearRecentlyWatchedVideosUseCase,
) : ViewModel() {
    internal val clearRecentlyWatchedEvent: StateFlow<StateEvent>
        field: MutableStateFlow<StateEvent> = MutableStateFlow(consumed)

    private fun getShowHiddenItemsFlow(): Flow<Boolean> = combine(
        monitorHiddenNodesEnabledUseCase().catch { Timber.e(it) },
        monitorShowHiddenItemsUseCase().catch { Timber.e(it) },
    ) { isHiddenNodesEnabled, isShowHiddenItems ->
        isShowHiddenItems || !isHiddenNodesEnabled
    }

    private fun combinedTriggerFlow(): Flow<Unit> = merge(
        monitorNodeUpdatesUseCase().filter {
            it.changes.keys.any { node ->
                node is FileNode && node.type is VideoFileTypeInfo
            }
        }.mapLatest { }
            .catch { Timber.e(it) },
        monitorOfflineNodeUpdatesUseCase().mapLatest { }
            .catch { Timber.e(it) },
    ).onStart { emit(Unit) }

    private fun getVideoRecentlyWatched(): Flow<Map<Long, List<VideoUiEntity>>> =
        combinedTriggerFlow().flatMapLatest {
            monitorVideoRecentlyWatchedUseCase().map { videoNodes ->
                videoNodes.map {
                    videoUiEntityMapper(it, emptyList())
                }.groupBy { TimeUnit.SECONDS.toDays(it.watchedDate) }
            }
        }

    internal val uiState: StateFlow<VideoRecentlyWatchedUiState> by lazy(LazyThreadSafetyMode.NONE) {
        combine(
            getVideoRecentlyWatched(),
            getShowHiddenItemsFlow()
        ) { data, showHiddenItems ->
            VideoRecentlyWatchedUiState.Data(
                groupedVideoRecentlyWatchedItems = data,
                showHiddenItems = showHiddenItems
            )
        }.asUiStateFlow(
            viewModelScope,
            VideoRecentlyWatchedUiState.Loading
        )
    }

    internal fun clearVideosRecentlyWatched() {
        viewModelScope.launch {
            runCatching {
                clearRecentlyWatchedVideosUseCase()
            }.onSuccess {
                clearRecentlyWatchedEvent.update { triggered }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    internal fun resetVideosRecentlyWatched() {
        clearRecentlyWatchedEvent.update { consumed }
    }
}