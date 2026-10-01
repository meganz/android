package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.FolderType
import mega.privacy.android.domain.entity.node.DefaultTypedFileNode
import mega.privacy.android.domain.entity.node.DefaultTypedFolderNode
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.extension.getNodeMappingStrategy
import mega.privacy.android.domain.extension.mapAsync
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetFolderTypeDataUseCase
import javax.inject.Inject

/**
 * Add nodes type use case
 *
 */
class AddNodesTypeUseCase @Inject constructor(
    private val getFolderTypeDataUseCase: GetFolderTypeDataUseCase,
    private val nodeRepository: NodeRepository,
) {
    /**
     * Invoke
     *
     * @param nodes
     */
    suspend operator fun invoke(nodes: List<UnTypedNode>): List<TypedNode> {
        val folderTypeData = if (nodes.any { it is FolderNode && it !is TypedNode }) {
            getFolderTypeDataUseCase()
        } else {
            null
        }
        return nodes.mapAsync(getNodeMappingStrategy(nodes.size)) { node ->
            when (node) {
                is TypedNode -> node
                is FileNode -> DefaultTypedFileNode(fileNode = node)
                is FolderNode -> DefaultTypedFolderNode(
                    folderNode = node,
                    type = folderTypeData?.let { nodeRepository.getFolderType(node, it) }
                        ?: FolderType.Default,
                )
            }
        }
    }
}
