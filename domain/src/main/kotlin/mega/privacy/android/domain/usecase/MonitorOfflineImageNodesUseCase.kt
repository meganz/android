package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import mega.privacy.android.domain.entity.Offline
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * Get offline nodes use case
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MonitorOfflineImageNodesUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val photosRepository: PhotosRepository,
) {
    operator fun invoke(path: String): Flow<List<ImageNode>> = nodeRepository
        .monitorOfflineNodeUpdates()
        .mapLatest { getImageNodes(path, it) }

    private suspend fun getImageNodes(path: String, offline: List<Offline>): List<ImageNode> {
        return offline.mapNotNull {
            if (it.path != path) {
                null
            } else {
                it.handle.toLongOrNull()?.let { handle ->
                    photosRepository.fetchImageNode(nodeId = NodeId(handle), filterSvg = false)
                }
            }
        }
    }
}
