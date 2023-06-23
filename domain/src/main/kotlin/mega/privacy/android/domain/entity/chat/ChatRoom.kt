package mega.privacy.android.domain.entity.chat

import mega.privacy.android.domain.entity.ChatRoomPermission

/**
 * Chat room item.
 *
 * @property chatId                  Handle identifying the chat.
 * @property ownPrivilege            Own privilege level in this chatroom [ChatRoomPermission].
 * @property numPreviewers           The number of previewers in this chat.
 * @property peerPrivilegesByHandles Map of peer privileges by their handles.
 * @property peerCount               Number of participants in the chat.
 * @property peerHandlesList         List of handles depending on the position of the peer.
 * @property peerPrivilegesList      List of peer privileges depending on the position of the peer.
 * @property isGroup                 If chat room is a group chat room.
 * @property isPublic                If chat room is public.
 * @property isPreview               If chat room is in preview mode.
 * @property authorizationToken      Get the authorization token in preview mode.
 *                                   This method returns an authorization token that can be used to authorize
 *                                   nodes received as attachments wle hiin preview mode, so the node can be
 *                                   downloaded/imported into the account via MegaApi::authorizeChatNode.
 *                                   If the chat is not in preview mode, this function will return NULL.
 * @property title                   Title of the chat if any.
 * @property hasCustomTitle          True if the chatroom has a customized title, false otherwise.
 * @property unreadCount             Number of unread messages for the chatroom.
 * @property userTyping              Handle of the user who is typing or has stopped typing a message.
 * @property userHandle              The handle of the user who has been joined/removed/change its name.
 *                                   This method return a valid value when hasChanged(CHANGE_TYPE_PARTICIPANTS) true.
 * @property isActive                True if the chat is active, false otherwise.
 *                                   Returns whether the user is member of the chatroom (for group chats),
 *                                   or the user is contact with the peer (for 1on1 chats).
 * @property isArchived              If chat room is archived.
 * @property retentionTime           Retention time.
 * @property creationTime            Returns the creation timestamp of the chat.
 * @property isMeeting               If chat room is a meeting.
 * @property isWaitingRoom           If chat room is a waiting room.
 * @property isOpenInvite            True if users with [ChatRoomPermission.Standard], can invite other users into the chat
 * @property isSpeakRequest          True if during calls, non moderator users, must request permission to speak.
 * @property changes                 Changes [ChatRoomChange].
 */
data class ChatRoom(
    val chatId: Long,
    val ownPrivilege: ChatRoomPermission,
    val numPreviewers: Long,
    val peerPrivilegesByHandles: Map<Long, ChatRoomPermission>,
    val peerCount: Long,
    val peerHandlesList: List<Long>,
    val peerPrivilegesList: List<ChatRoomPermission>,
    val isGroup: Boolean,
    val isPublic: Boolean,
    val isPreview: Boolean,
    val authorizationToken: String?,
    val title: String,
    val hasCustomTitle: Boolean,
    val unreadCount: Int,
    val userTyping: Long,
    val userHandle: Long,
    val isActive: Boolean,
    val isArchived: Boolean,
    val retentionTime: Long,
    val creationTime: Long,
    val isMeeting: Boolean,
    val isWaitingRoom: Boolean,
    val isOpenInvite: Boolean,
    val isSpeakRequest: Boolean,
    val changes: List<ChatRoomChange>? = null,
) {
    /**
     * Checks if the chat has a change.
     *
     * @param change [ChatRoomChange] to check.
     * @return True if the chat has the change in question, false otherwise.
     */
    fun hasChanged(change: ChatRoomChange) = changes?.contains(change) == true
}