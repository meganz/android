package mega.privacy.android.domain.entity.node.publiclink

import mega.privacy.android.domain.entity.node.TypedFolderNode

/**
 * Public link folder
 *
 * @property node
 * @property parent
 * @property fetchChildrenFunction
 * @constructor Create empty Public link folder
 */
class PublicLinkFolder(
    val node: TypedFolderNode,
    override val parent: PublicLinkFolder?,
    fetchChildrenForParent: suspend (PublicLinkFolder) -> List<PublicLinkNode>,
) : PublicLinkNode, TypedFolderNode by node {
    val getChildPublicLinkNodes = suspend { fetchChildrenForParent(this) }
}
