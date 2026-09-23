package mega.privacy.android.domain.entity.node.publiclink

import mega.privacy.android.domain.entity.node.TypedFileNode

/**
 * Public link file
 *
 * @property node
 * @constructor Create empty Public link file
 */
class PublicLinkFile(
    val node: TypedFileNode,
) : PublicLinkNode, TypedFileNode by node
