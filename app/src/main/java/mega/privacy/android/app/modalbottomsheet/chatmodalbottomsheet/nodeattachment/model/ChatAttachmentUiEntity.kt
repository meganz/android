package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.nodeattachment.model

import mega.privacy.android.domain.entity.node.NodeId

/**
 * UI entity for chat attachment
 *
 * @property nodeId the node id
 * @property name the name of the attachment
 * @property size the size of the attachment
 * @property thumbnailPath the path of the thumbnail
 * @property isInAnonymousMode true if the attachment is in anonymous mode
 * @property isAvailableOffline true if the attachment is available offline
 */
data class ChatAttachmentUiEntity(
    val nodeId: NodeId,
    val name: String,
    val size: Long,
    val thumbnailPath: String?,
    val isInAnonymousMode: Boolean,
    val isAvailableOffline: Boolean
)