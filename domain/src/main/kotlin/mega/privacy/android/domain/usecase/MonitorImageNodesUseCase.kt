package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.ImageFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * Get images nodes use case
 */
class MonitorImageNodesUseCase @Inject constructor(
    private val photosRepository: PhotosRepository,
    private val nodeRepository: NodeRepository,
) {
    private val nodesCache: MutableMap<NodeId, ImageNode> = mutableMapOf()

    private val constraints: List<suspend (Node, List<NodeId>) -> Boolean> = listOf(
        ::checkMediaNode,
        ::checkCollectionNodes,
    )

    private suspend fun checkMediaNode(node: Node, nodeIds: List<NodeId>): Boolean {
        return node is FileNode && (node.type is ImageFileTypeInfo || node.type is VideoFileTypeInfo)
    }

    private suspend fun checkCollectionNodes(node: Node, nodeIds: List<NodeId>): Boolean {
        return node.id in nodeIds
    }

    /**
     * Invoke use case
     */
    operator fun invoke(nodeIds: List<NodeId>): Flow<List<ImageNode>> = flow {
        emit(populateNodes(nodeIds))
        emitAll(monitorNodes(nodeIds))
    }

    private suspend fun populateNodes(nodeIds: List<NodeId>): List<ImageNode> {
        val nodes = nodeIds.mapNotNull { nodeId ->
            photosRepository.fetchImageNode(nodeId, filterSvg = false)
        }

        nodesCache.clear()
        nodesCache.putAll(nodes.associateBy { it.id })

        return nodesCache.values.toList()
    }

    private fun monitorNodes(nodeIds: List<NodeId>): Flow<List<ImageNode>> {
        return nodeRepository.monitorNodeUpdates()
            .map { nodeUpdate ->
                updateNodes(nodeIds, nodeUpdate)
            }
    }

    private suspend fun updateNodes(
        nodeIds: List<NodeId>,
        nodeUpdate: NodeUpdate,
    ): List<ImageNode> {
        for (node in nodeUpdate.changes.keys) {
            if (constraints.any { !it(node, nodeIds) }) {
                nodesCache.remove(node.id)
                continue
            }

            val newNode = photosRepository.fetchImageNode(nodeId = node.id)
            if (newNode == null) {
                nodesCache.remove(node.id)
            } else {
                nodesCache[newNode.id] = newNode
            }
        }
        return nodesCache.values.toList()
    }
}
