package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.exception.ParentNotAFolderException
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Get Photos from a folder by  its id
 */
class GetTypedNodesFromFolderUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val addNodeType: AddNodeType,
) {

    operator fun invoke(folderId: NodeId): Flow<List<TypedNode>> {
        return flow {
            val nodes = getChildren(folderId)
            emit(nodes)
            val nodeIds = nodes.map { it.id } + folderId
            emitAll(getMonitoredList(folderId, nodeIds))
        }.mapLatest { nodeList ->
            nodeList.map { addNodeType(it) }
        }
    }

    private suspend fun getChildren(folderId: NodeId): List<UnTypedNode> {
        val folder = nodeRepository.getNodeById(folderId) as? FolderNode
            ?: throw ParentNotAFolderException("Attempted to fetch folder node: $folderId")
        return nodeRepository.getNodeChildren(folder)

    }

    private fun getMonitoredList(folderId: NodeId, nodeIds: List<NodeId>) =
        nodeRepository.monitorNodeUpdates()
            .filter { changes ->
                changes.changes.keys.map { it.id }
                    .intersect(
                        nodeIds.toSet()
                    ).isNotEmpty()
            }.map { getChildren(folderId) }

}