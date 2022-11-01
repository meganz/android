package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import mega.privacy.android.domain.repository.FileRepository
import javax.inject.Inject

/**
 * Get Photos from a folder by its id
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetTypedNodesFromFolder @Inject constructor(
    private val fileRepository: FileRepository,
    private val addNodeType: AddNodeType,
) : GetTypedNodesFromFolder {

    override fun invoke(folderNodeId: NodeId): Flow<List<TypedNode>> {
        return flow {
            val nodes = getChildren(folderNodeId)
            emit(nodes)
            val nodeIds = nodes.map { it.id } + folderNodeId
            emitAll(getMonitoredList(folderNodeId, nodeIds))
        }.mapLatest { nodeList ->
            nodeList.map { addNodeType(it) }
        }
    }

    private suspend fun getChildren(folderId: NodeId): List<UnTypedNode> {
        val folder = fileRepository.getNodeById(folderId) as? FolderNode
            ?: throw ParentNotAFolderException("Attempted to fetch folder node: $folderId")
        return fileRepository.getNodeChildren(folder)

    }

    private fun getMonitoredList(folderId: NodeId, nodeIds: List<NodeId>) =
        fileRepository.monitorNodeUpdates()
            .filter { changes ->
                changes.map { it.id }
                    .intersect(
                        nodeIds.toSet()
                    ).isNotEmpty()
            }.map { getChildren(folderId) }

}