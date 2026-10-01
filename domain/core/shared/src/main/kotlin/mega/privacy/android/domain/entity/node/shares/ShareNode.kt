package mega.privacy.android.domain.entity.node.shares

import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * Share Node, used in outgoing and incoming shares
 *
 * @property shareData [ShareData] associated with the node
 * @constructor Create empty ShareNode
 */
sealed interface ShareNode : TypedNode {
    val shareData: ShareData?
}