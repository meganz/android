package mega.privacy.android.domain.entity.photos

import mega.privacy.android.domain.entity.node.NodeId

/**
 * A lightweight image node reference (id + name) used to order the viewer's pager and resolve the
 * full node by [id] on demand.
 */
data class ImageNodeInfo(
    val id: NodeId,
    val name: String,
)
