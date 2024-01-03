package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * Get favourite nodes use case
 */
class MonitorFavouriteImageNodesUseCase @Inject constructor(
    private val photosRepository: PhotosRepository,
    private val nodeRepository: NodeRepository,
) {
    @Volatile
    private var nodesCache: List<ImageNode> = listOf()

    /**
     * Invoke use case
     */
    operator fun invoke(nodeId: NodeId, count: Int): Flow<List<ImageNode>> = flow {
        emit(populateNodes(nodeId, count))
        emitAll(monitorNodes(nodeId, count))
    }

    private suspend fun populateNodes(nodeId: NodeId, count: Int): List<ImageNode> {
        val node = photosRepository.fetchImageNode(nodeId, filterSvg = false)

        nodesCache = if (node?.isFavourite == true) {
            listOf(node)
        } else {
            emptyList()
        }
        return nodesCache
    }

    private fun monitorNodes(nodeId: NodeId, count: Int): Flow<List<ImageNode>> {
        return nodeRepository.monitorNodeUpdates()
            .map { nodeUpdate ->
                updateNodes(nodeId, count, nodeUpdate)
            }
    }

    private suspend fun updateNodes(
        nodeId: NodeId,
        count: Int,
        nodeUpdate: NodeUpdate,
    ): List<ImageNode> {
        for (node in nodeUpdate.changes.keys) {
            if (node.id != nodeId) continue
            populateNodes(nodeId, count)
        }
        return nodesCache
    }
}
