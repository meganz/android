package mega.privacy.android.domain.entity.node.shares

import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * Share Node, used in outgoing and incoming shares
 *
 * @property node
 * @property shareData
 * @constructor Create empty ShareNode
 */
class ShareNode(
    val node: TypedNode,
    val shareData: ShareData? = null,
) : TypedNode by node