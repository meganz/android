package mega.privacy.android.domain.entity

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.UnTypedNode

/**
 * RecentActionBucketUnTyped
 *
 * @property timestamp
 * @property userEmail
 * @property parentNodeId
 * @property isUpdate
 * @property isMedia
 * @property nodes
 */
data class RecentActionBucketUnTyped(
    val timestamp: Long,
    val userEmail: String,
    val parentNodeId: NodeId,
    val isUpdate: Boolean,
    val isMedia: Boolean,
    val nodes: List<UnTypedNode>,
)
