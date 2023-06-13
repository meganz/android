package mega.privacy.android.domain.entity.chat

import mega.privacy.android.domain.entity.ChatRoomPermission

/**
 * Data class as entity for chat messages.
 *
 * @property status [ChatMessageStatus]
 * @property msgId Identifier of the message
 * @property tempId Temporal identifier of the message
 *                  This has different usages depending on the status of the message:
 *                   - ChatMessageStatus.SENDING: valid until it's confirmed by the server.
 *                   - ChatMessageStatus.SENDING_MANUAL: valid until it's removed from manual-send queue.
 *                  If status is ChatMessageStatus.SENDING_MANUAL, this value can be used to identify the
 *                  message moved into the manual-send queue. The message itself will be identified
 *                  by its rowId from now on. The row id can be passed to MegaChatApi::removeUnsentMessage
 *                  to definitely remove the message.
 *                  For messages in a different status than above, this identifier should not be used.
 * @property msgIndex Index of the message in the loaded history.
 *                    The higher is the value of the index, the newer is the chat message.
 *                    The lower is the value of the index, the older is the chat message.
 * @property userHandle Chat handle.
 *                      If type returns ChatMessageType.SCHED_MEETING, this method returns the
 *                      scheduled meeting id, of the updated scheduled meeting.
 *                      If MegaChatMessage::getType doesn't returns ChatMessageType.SCHED_MEETING,
 *                      this method returns:
 *                       - For outgoing messages, the handle of the target user.
 *                       - For incoming messages, the handle of the sender.
 * @property type [ChatMessageType]
 * @property hasConfirmedReactions If the message has any confirmed reaction
 * @property timestamp Timestamp of the message
 * @property content Content of the message. If message was deleted, it returns NULL.
 * @property isEdited Whether the message is an edit of the original message
 * @property isDeleted Whether the message has been deleted
 * @property isEditable Whether the message can be edited
 * @property isDeletable Whether the message can be deleted
 * @property isManagementMessage Whether the message is a management message.
 *                               Management messages are intended to record in the history any change related
 *                               to the management of the chatroom, such as a title change or an addition of a peer.
 * @property privilege [ChatRoomPermission] Return the privilege of the user relative to the action.
 * @property code [ChatMessageCode]
 * @property usersCount Number of user that have been attached to the message
 * @property userHandles Handles of the users that has been attached.
 *                       Only valid for messages with ChatMessageType.CONTACT_ATTACHMENT
 * @property userNames Names of the users that has been attached.
 *                     Only valid for messages with ChatMessageType.CONTACT_ATTACHMENT
 * @property userEmails Emails of the users that has been attached.
 *                      Only valid for messages with ChatMessageType.CONTACT_ATTACHMENT
 * @property duration Call duration in seconds.
 *                    Only valid for messages with ChatMessageType.CALL_ENDED
 * @property retentionTime Retention time in seconds.
 *                         Only valid for messages with ChatMessageType.SET_RETENTION_TIME
 * @property termCode [ChatMessageTermCode]
 *                    Only valid for messages with ChatMessageType.CALL_ENDED
 * @property rowId Id for messages in manual sending status / queue
 */
data class ChatMessage(
    val status: ChatMessageStatus,
    val msgId: Long,
    val tempId: Long,
    val msgIndex: Int,
    val userHandle: Long,
    val type: ChatMessageType,
    val hasConfirmedReactions: Boolean,
    val timestamp: Long,
    val content: String?,
    val isEdited: Boolean,
    val isDeleted: Boolean,
    val isEditable: Boolean,
    val isDeletable: Boolean,
    val isManagementMessage: Boolean,
    val privilege: ChatRoomPermission,
    val code: ChatMessageCode,
    val usersCount: Long,
    val userHandles: List<Long>,
    val userNames: List<String>,
    val userEmails: List<String>,
    val duration: Int,
    val retentionTime: Long,
    val termCode: ChatMessageTermCode,
    val rowId: Long,
)
