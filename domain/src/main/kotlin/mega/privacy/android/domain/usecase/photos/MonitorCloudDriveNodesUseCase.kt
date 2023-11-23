package mega.privacy.android.domain.usecase.photos

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
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import javax.inject.Inject

private typealias NodeCheck = suspend (Node, NodeId) -> Boolean

/**
 * Use case to monitor timeline nodes
 */
class MonitorCloudDriveNodesUseCase @Inject constructor(
    private val photosRepository: PhotosRepository,
    private val nodeRepository: NodeRepository,
    private val getCloudSortOrder: GetCloudSortOrder,
) {
    private val nodesCache: MutableMap<NodeId, ImageNode> = mutableMapOf()

    private val constraints: List<NodeCheck> = listOf(
        ::checkMediaNode,
        ::checkCloudDriveNode,
    )

    private suspend fun checkMediaNode(node: Node, targetParentId: NodeId): Boolean {
        return node is FileNode && (node.type is ImageFileTypeInfo || node.type is VideoFileTypeInfo)
    }

    private suspend fun checkCloudDriveNode(node: Node, targetParentId: NodeId): Boolean {
        return node.parentId == targetParentId
    }

    operator fun invoke(parentId: NodeId): Flow<List<ImageNode>> = flow {
        emit(populateNodes(parentId))
        emitAll(monitorNodes(parentId))
    }

    private suspend fun populateNodes(parentId: NodeId): List<ImageNode> {
        val sortOrder = getCloudSortOrder()
        val nodes = photosRepository.getCloudDriveImageNodes(parentId, sortOrder)

        nodesCache.clear()
        nodesCache.putAll(nodes.associateBy { it.id })

        return nodesCache.values.toList()
    }

    private suspend fun monitorNodes(parentId: NodeId): Flow<List<ImageNode>> {
        return nodeRepository.monitorNodeUpdates()
            .map { nodeUpdate ->
                updateNodes(parentId, nodeUpdate)
            }
    }

    private suspend fun updateNodes(parentId: NodeId, nodeUpdate: NodeUpdate): List<ImageNode> {
        for (node in nodeUpdate.changes.keys) {
            if (constraints.any { !it(node, parentId) }) continue
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
