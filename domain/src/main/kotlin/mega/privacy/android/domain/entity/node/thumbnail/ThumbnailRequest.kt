package mega.privacy.android.domain.entity.node.thumbnail

import mega.privacy.android.domain.entity.node.NodeId

/**
 * Thumbnail request
 *
 * @property id handle of node
 * @property isPublicNode is public node
 */
data class ThumbnailRequest(val id: NodeId, val isPublicNode: Boolean = false)