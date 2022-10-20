package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.node.DefaultTypedFileNode
import mega.privacy.android.domain.entity.node.DefaultTypedFolderNode
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.TypedNode
import javax.inject.Inject

class DefaultAddNodeType @Inject constructor(private val getFolderType: GetFolderType) :
    AddNodeType {
    override suspend fun invoke(node: Node): TypedNode {
        return when (node) {
            is FileNode -> DefaultTypedFileNode(node)
            is FolderNode -> DefaultTypedFolderNode(node, getFolderType(node))
        }
    }
}