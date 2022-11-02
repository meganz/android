package mega.privacy.android.domain.entity

import mega.privacy.android.domain.entity.node.Node

/**
 * RecentActionBucket
 *
 * @property timestamp
 * @property userEmail
 * @property parentHandle
 * @property isUpdate
 * @property isMedia
 * @property nodes
 */
data class RecentActionBucket(
    val timestamp: Long,
    val userEmail: String,
    val parentHandle: Long,
    val isUpdate: Boolean,
    val isMedia: Boolean,
    val nodes: List<Node>,
)
