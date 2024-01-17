package mega.privacy.android.domain.entity.chat

import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.node.Node

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
 * @property handleOfAction The handle of the user relative to the action. Only valid for management messages:
 *                           - ChatMessageType.ALTER_PARTICIPANTS: handle of the user who is added/removed
 *                           - ChatMessageType.PRIV_CHANGE: handle of the user whose privilege is changed
 *                           - ChatMessageType.REVOKE_ATTACHMENT: handle of the node which access has been revoked
 *                           - ChatMessageType.SCHED_MEETING: scheduled meeting handle of the updated scheduled meeting
 * @property privilege [ChatRoomPermission] Return the privilege of the user relative to the action.
 * @property code [ChatMessageCode]
 * @property usersCount Number of user that have been attached to the message
 * @property userHandles Handles of the users that has been attached.
 *                       Only valid for messages with ChatMessageType.CONTACT_ATTACHMENT
 * @property userNames Names of the users that has been attached.
 *                     Only valid for messages with ChatMessageType.CONTACT_ATTACHMENT
 * @property userEmails Emails of the users that has been attached.
 *                      Only valid for messages with ChatMessageType.CONTACT_ATTACHMENT
 * @property handleList List of handles. It can be used for different purposes.
 *                       - ChatMessageType.CALL_ENDED
 *                         It will be empty if termCode is not ChatMessageTermCode.ENDED either ChatMessageTermCode.FAILED
 * @property nodeList List with all [Node] attached to the message.
 * @property duration Call duration in seconds.
 *                    Only valid for messages with ChatMessageType.CALL_ENDED
 * @property retentionTime Retention time in seconds.
 *                         Only valid for messages with ChatMessageType.SET_RETENTION_TIME
 * @property termCode [ChatMessageTermCode]
 *                    Only valid for messages with ChatMessageType.CALL_ENDED
 * @property rowId Id for messages in manual sending status / queue
 * @property changes
 * @property containsMeta [ContainsMeta]
 */
data class ChatMessage(
    override val status: ChatMessageStatus,
    override val msgId: Long,
    override val tempId: Long,
    override val msgIndex: Int,
    override val userHandle: Long,
    override val type: ChatMessageType,
    override val hasConfirmedReactions: Boolean,
    override val timestamp: Long,
    override val content: String?,
    override val isEdited: Boolean,
    override val isDeleted: Boolean,
    override val isEditable: Boolean,
    override val isDeletable: Boolean,
    override val isManagementMessage: Boolean,
    override val handleOfAction: Long,
    override val privilege: ChatRoomPermission,
    override val code: ChatMessageCode,
    override val usersCount: Long,
    override val userHandles: List<Long>,
    override val userNames: List<String>,
    override val userEmails: List<String>,
    val nodeList: List<Node>,
    override val handleList: List<Long>,
    override val duration: Int,
    override val retentionTime: Long,
    override val termCode: ChatMessageTermCode,
    override val rowId: Long,
    override val changes: List<ChatMessageChange>,
    val containsMeta: ContainsMeta?,
) : ChatMessageInfo {

    /**
     * Checks if the message has changed with the received change.
     *
     * @param chatMessageChange [ChatMessageChange]
     * @return True if the message has changed with the specified change, false otherwise.
     */
    fun hasChanged(chatMessageChange: ChatMessageChange) = changes.contains(chatMessageChange)
}
