package mega.privacy.android.domain.entity

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.UnTypedNode

/**
 * RecentActionBucketUnTyped
 *
 * @property identifier An unique identifier generated based on the data of the bucket, it can be used to identify a bucket from recent actions list
 * @property timestamp
 * @property dateTimestamp Timestamp of date only (time set to 00:00:00)
 * @property userEmail
 * @property parentNodeId
 * @property isUpdate
 * @property isMedia
 * @property nodes
 */
data class RecentActionBucketUnTyped(
    val identifier: String,
    val timestamp: Long,
    val dateTimestamp: Long,
    val userEmail: String,
    val parentNodeId: NodeId,
    val isUpdate: Boolean,
    val isMedia: Boolean,
    val nodes: List<UnTypedNode>,
)
