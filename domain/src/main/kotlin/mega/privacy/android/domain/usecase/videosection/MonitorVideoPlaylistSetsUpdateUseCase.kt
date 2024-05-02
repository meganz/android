package mega.privacy.android.domain.usecase.videosection

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.repository.VideoSectionRepository
import javax.inject.Inject

/**
 * Use case for monitoring video playlist sets update
 */
class MonitorVideoPlaylistSetsUpdateUseCase @Inject constructor(
    private val videoSectionRepository: VideoSectionRepository,
    private val nodeRepository: NodeRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    /**
     * Invoke
     *
     * @return a flow of video playlist sets update
     */
    operator fun invoke() = merge(
        videoSectionRepository.monitorSetsUpdates(),
        monitorNodeUpdates(),
        monitorOfflineNodeUpdates()
    )

    private fun monitorNodeUpdates(): Flow<List<Long>> =
        nodeRepository.monitorNodeUpdates()
            .mapNotNull { nodeUpdate ->
                val videoSetsMap = videoSectionRepository.getVideoSetsMap()
                val videoPlaylistsMap = videoSectionRepository.getVideoPlaylistsMap()
                nodeUpdate.changes.keys.flatMap { node ->
                    val setIds = videoSetsMap[node.id] ?: emptySet()
                    setIds.mapNotNull { videoPlaylistsMap[it]?.id }
                }
            }.flowOn(defaultDispatcher)

    private fun monitorOfflineNodeUpdates(): Flow<List<Long>> =
        nodeRepository.monitorOfflineNodeUpdates()
            .mapNotNull { offlineList ->
                offlineList.map { it.handle.toLong() }
            }
}