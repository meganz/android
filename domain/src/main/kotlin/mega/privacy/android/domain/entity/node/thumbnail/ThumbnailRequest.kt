package mega.privacy.android.domain.entity.node.thumbnail

import mega.privacy.android.domain.entity.node.NodeId

/**
 * data to load thumbnail
 */
sealed interface ThumbnailData

/**
 * Thumbnail request
 *
 * @property id handle of node
 * @property isPublicNode is public node
 */
data class ThumbnailRequest(val id: NodeId, val isPublicNode: Boolean = false) : ThumbnailData {
    companion object {
        /**
         * Create a ThumbnailRequest from a handle
         */
        @JvmStatic
        fun fromHandle(handle: Long) = ThumbnailRequest(NodeId(handle))
    }
}

/**
 * Chat thumbnail request
 *
 * @property chatId handle of chat
 * @property messageId handle of message
 */
data class ChatThumbnailRequest(val chatId: Long, val messageId: Long) : ThumbnailData