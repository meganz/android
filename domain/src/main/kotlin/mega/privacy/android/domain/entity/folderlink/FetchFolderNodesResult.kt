package mega.privacy.android.domain.entity.folderlink

import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * Class representing result of fetch node use case
 *
 * @property rootNode
 * @property childrenNodes
 */
data class FetchFolderNodesResult(
    var rootNode: TypedFolderNode? = null,
    var childrenNodes: List<TypedNode> = listOf()
)