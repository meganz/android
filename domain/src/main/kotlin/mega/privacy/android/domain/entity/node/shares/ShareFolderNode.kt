package mega.privacy.android.domain.entity.node.shares

import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.node.TypedFolderNode

/**
 * Share Folder Node
 *
 * @property node
 * @property shareData
 * @constructor Create empty ShareFolderNode
 */
class ShareFolderNode(
    val node: TypedFolderNode,
    override val shareData: ShareData? = null,
) : ShareNode, TypedFolderNode by node