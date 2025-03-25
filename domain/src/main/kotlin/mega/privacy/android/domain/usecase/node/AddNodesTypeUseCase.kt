package mega.privacy.android.domain.usecase.node

import mega.privacy.android.domain.entity.node.DefaultTypedFileNode
import mega.privacy.android.domain.entity.node.DefaultTypedFolderNode
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.usecase.GetFolderType
import javax.inject.Inject

/**
 * Add nodes type use case
 *
 */
class AddNodesTypeUseCase @Inject constructor(
    private val getFolderType: GetFolderType,
) {
    /**
     * Invoke
     *
     * @param nodes
     */
    suspend operator fun invoke(nodes: List<UnTypedNode>): List<TypedNode> {
        return nodes.map { node ->
            when (node) {
                is TypedNode -> node
                is FileNode -> DefaultTypedFileNode(fileNode = node)
                is FolderNode -> DefaultTypedFolderNode(
                    folderNode = node, type = getFolderType(node)
                )
            }
        }
    }
}