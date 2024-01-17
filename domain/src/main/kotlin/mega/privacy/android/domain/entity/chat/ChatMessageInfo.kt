package mega.privacy.android.domain.entity.chat

import mega.privacy.android.domain.entity.ChatRoomPermission

/**
 * @property status
 * @property msgId
 * @property tempId
 * @property msgIndex
 * @property userHandle
 * @property type
 * @property hasConfirmedReactions
 * @property timestamp
 * @property content
 * @property isEdited
 * @property isDeleted
 * @property isEditable
 * @property isDeletable
 * @property isManagementMessage
 * @property handleOfAction
 * @property privilege
 * @property code
 * @property usersCount
 * @property userHandles
 * @property userNames
 * @property userEmails
 * @property nodeList
 * @property handleList
 * @property duration
 * @property retentionTime
 * @property termCode
 * @property rowId
 * @property changes
 **/
interface ChatMessageInfo {
    val status: ChatMessageStatus
    val msgId: Long
    val tempId: Long
    val msgIndex: Int
    val userHandle: Long
    val type: ChatMessageType
    val hasConfirmedReactions: Boolean
    val timestamp: Long
    val content: String?
    val isEdited: Boolean
    val isDeleted: Boolean
    val isEditable: Boolean
    val isDeletable: Boolean
    val isManagementMessage: Boolean
    val handleOfAction: Long
    val privilege: ChatRoomPermission
    val code: ChatMessageCode
    val usersCount: Long
    val userHandles: List<Long>
    val userNames: List<String>
    val userEmails: List<String>
    val handleList: List<Long>
    val duration: Int
    val retentionTime: Long
    val termCode: ChatMessageTermCode
    val rowId: Long
    val changes: List<ChatMessageChange>
}