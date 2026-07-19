package mega.privacy.android.data.test.stub

import nz.mega.sdk.MegaChatListItem
import nz.mega.sdk.MegaChatRoom

/**
 * In-memory stub of [MegaChatListItem] that is safe to use in unit tests.
 *
 * Every public instance method is overridden, so no call ever reaches the native SDK
 * (which would crash the JVM given the null native pointer). Data getters are backed by
 * constructor parameters with sensible defaults; all other methods return benign constants.
 */
class StubMegaChatListItem(
    private val chatId: Long = -1L,
    private val title: String = "",
    private val ownPrivilege: Int = MegaChatRoom.PRIV_STANDARD,
    private val unreadCount: Int = 0,
    private val lastMessage: String? = null,
    private val lastMessageId: Long = -1L,
    private val lastMessageType: Int = 0,
    private val lastMessageSender: Long = -1L,
    private val lastTimestamp: Long = 0L,
    private val isGroup: Boolean = false,
    private val isPublic: Boolean = false,
    private val isNoteToSelf: Boolean = false,
    private val isPreview: Boolean = false,
    private val isActive: Boolean = true,
    private val isArchived: Boolean = false,
    private val isDeleted: Boolean = false,
    private val isCallInProgress: Boolean = false,
    private val isMeeting: Boolean = false,
    private val peerHandle: Long = -1L,
    private val lastMessagePriv: Int = 0,
    private val lastMessageHandle: Long = -1L,
    private val numPreviewers: Long = 0L,
    private val changes: Int = 0,
) : MegaChatListItem(0, false) {

    override fun delete() = Unit

    override fun getChanges(): Int = changes
    override fun hasChanged(p0: Int): Boolean = (changes and p0) != 0
    override fun getChatId(): Long = chatId
    override fun getTitle(): String = title
    override fun getOwnPrivilege(): Int = ownPrivilege
    override fun getUnreadCount(): Int = unreadCount
    override fun getLastMessage(): String? = lastMessage
    override fun getLastMessageId(): Long = lastMessageId
    override fun getLastMessageType(): Int = lastMessageType
    override fun getLastMessageSender(): Long = lastMessageSender
    override fun getLastTimestamp(): Long = lastTimestamp
    override fun isGroup(): Boolean = isGroup
    override fun isPublic(): Boolean = isPublic
    override fun isNoteToSelf(): Boolean = isNoteToSelf
    override fun isPreview(): Boolean = isPreview
    override fun isActive(): Boolean = isActive
    override fun isArchived(): Boolean = isArchived
    override fun isDeleted(): Boolean = isDeleted
    override fun isCallInProgress(): Boolean = isCallInProgress
    override fun getPeerHandle(): Long = peerHandle
    override fun getLastMessagePriv(): Int = lastMessagePriv
    override fun getLastMessageHandle(): Long = lastMessageHandle
    override fun getNumPreviewers(): Long = numPreviewers
    override fun isMeeting(): Boolean = isMeeting
}
