package mega.privacy.android.domain.entity

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.UnTypedNode

/**
 * RecentActionBucketUnTyped
 *
 * @property id
 * @property timestamp
 * @property dateTimestamp Timestamp of date only (time set to 00:00:00)
 * @property userEmail
 * @property parentNodeId
 * @property isUpdate
 * @property isMedia
 * @property nodes
 */
data class RecentActionBucketUnTyped(
    val id: String,
    val timestamp: Long,
    val dateTimestamp: Long,
    val userEmail: String,
    val parentNodeId: NodeId,
    val isUpdate: Boolean,
    val isMedia: Boolean,
    val nodes: List<UnTypedNode>,
)
