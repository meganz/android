package mega.privacy.android.domain.entity.home

import mega.privacy.android.domain.entity.node.NodeId

/**
 * A folder or file pinned to the Home screen. [name] and [isFolder] are a fast-render snapshot;
 * the live node is resolved at display time. Items are shown oldest-pinned first ([pinnedAt]).
 */
data class PinnedHomeItem(
    val nodeId: NodeId,
    val name: String,
    val isFolder: Boolean,
    val pinnedAt: Long = System.currentTimeMillis(),
)
