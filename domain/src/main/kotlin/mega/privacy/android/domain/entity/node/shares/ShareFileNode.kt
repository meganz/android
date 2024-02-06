package mega.privacy.android.domain.entity.node.shares

import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.node.TypedFileNode

/**
 * Share File Node
 *
 * @property node
 * @property shareData
 * @constructor Create empty ShareFileNode
 */
class ShareFileNode(
    val node: TypedFileNode,
    override val shareData: ShareData? = null,
) : ShareNode, TypedFileNode by node