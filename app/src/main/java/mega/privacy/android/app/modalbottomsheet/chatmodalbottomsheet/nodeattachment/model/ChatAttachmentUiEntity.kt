package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.nodeattachment.model

/**
 * UI entity for chat attachment
 * @property name the name of the attachment
 * @property size the size of the attachment
 * @property thumbnailPath the path of the thumbnail
 * @property modificationTime the modification time of the attachment
 * @property isInAnonymousMode true if the attachment is in anonymous mode
 * @property isAvailableOffline true if the attachment is available offline
 */
data class ChatAttachmentUiEntity(
    val name: String,
    val size: Long,
    val thumbnailPath: String?,
    val isInAnonymousMode: Boolean,
    val isAvailableOffline: Boolean
)