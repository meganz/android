package mega.privacy.android.domain.entity.node.publiclink

import mega.privacy.android.domain.entity.node.TypedFileNode

/**
 * Public link file
 *
 * @property node
 * @property parent
 * @constructor Create empty Public link file
 */
class PublicLinkFile(
    val node: TypedFileNode,
    override val parent: PublicLinkFolder?,
) : PublicLinkNode, TypedFileNode by node {
}