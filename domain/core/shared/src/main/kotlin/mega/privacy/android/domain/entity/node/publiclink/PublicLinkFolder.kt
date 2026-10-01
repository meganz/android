package mega.privacy.android.domain.entity.node.publiclink

import mega.privacy.android.domain.entity.node.TypedFolderNode

/**
 * Public link folder
 *
 * @property node
 * @constructor Create empty Public link folder
 */
class PublicLinkFolder(
    val node: TypedFolderNode,
) : PublicLinkNode, TypedFolderNode by node
