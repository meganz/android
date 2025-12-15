package mega.privacy.android.domain.entity

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode

/**
 * RecentActionBucket
 *
 * @property identifier An unique identifier based on the data of the bucket, it can be used to identify a bucket
 * @property timestamp
 * @property userEmail
 * @property parentNodeId
 * @property isUpdate
 * @property isMedia
 * @property nodes
 * @property userName
 * @property parentFolderName
 * @property parentFolderSharesType
 * @property currentUserIsOwner
 * @property isKeyVerified
 */
data class RecentActionBucket(
    val identifier: String,
    val timestamp: Long,
    val userEmail: String,
    val parentNodeId: NodeId,
    val isUpdate: Boolean,
    val isMedia: Boolean,
    val nodes: List<TypedFileNode>,
    val userName: String = "",
    val parentFolderName: String = "",
    val parentFolderSharesType: RecentActionsSharesType = RecentActionsSharesType.NONE,
    val currentUserIsOwner: Boolean = false,
    val isKeyVerified: Boolean = false,
    val isNodeKeyDecrypted: Boolean = true
)

/**
 * Shares type of the parent folder of a node in the recent actions
 */
enum class RecentActionsSharesType {
    /**
     * Not a shared node
     */
    NONE,

    /**
     * Node in incoming shares
     */
    INCOMING_SHARES,

    /**
     * Node in outgoing shares
     */
    OUTGOING_SHARES,

    /**
     * Node in pending outgoing shares
     */
    PENDING_OUTGOING_SHARES,
}
