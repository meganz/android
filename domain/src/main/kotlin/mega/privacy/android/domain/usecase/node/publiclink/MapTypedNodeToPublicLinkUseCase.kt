package mega.privacy.android.domain.usecase.node.publiclink

import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFile
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFolder
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkNode
import javax.inject.Inject

/**
 * Map typed node to public link use case
 *
 * Maps an already-typed node to its public link equivalent ([PublicLinkFile] or [PublicLinkFolder]).
 * Unlike [MapNodeToPublicLinkUseCase], this use case accepts a [TypedNode] directly, avoiding a
 * redundant [mega.privacy.android.domain.usecase.AddNodeType] call when the node is already typed.
 */
class MapTypedNodeToPublicLinkUseCase @Inject constructor() {
    /**
     * Invoke
     *
     * @param node already-typed node to map
     * @return [PublicLinkNode]
     */
    operator fun invoke(node: TypedNode): PublicLinkNode =
        when (node) {
            is TypedFileNode -> PublicLinkFile(node)
            is TypedFolderNode -> PublicLinkFolder(node)
            else -> throw IllegalStateException("Invalid type: $node")
        }
}
