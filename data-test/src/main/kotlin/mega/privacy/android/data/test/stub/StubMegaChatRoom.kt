package mega.privacy.android.data.test.stub

import nz.mega.sdk.MegaChatRoom

/**
 * In-memory stub of [MegaChatRoom] that is safe to use in unit tests.
 *
 * Every public instance method is overridden, so no call ever reaches the native SDK
 * (which would crash the JVM given the null native pointer). Data getters are backed by
 * constructor parameters with sensible defaults; all other methods return benign constants.
 */
class StubMegaChatRoom(
    private val chatId: Long = -1L,
    private val title: String = "",
    private val ownPrivilege: Int = MegaChatRoom.PRIV_STANDARD,
    private val unreadCount: Int = 0,
    private val peers: List<Pair<Long, Int>> = emptyList(),
    private val isGroup: Boolean = false,
    private val isPublic: Boolean = false,
    private val isMeeting: Boolean = false,
    private val isNoteToSelf: Boolean = false,
    private val isPreview: Boolean = false,
    private val isActive: Boolean = true,
    private val isArchived: Boolean = false,
    private val isWaitingRoom: Boolean = false,
    private val isOpenInvite: Boolean = false,
    private val isSpeakRequest: Boolean = false,
    private val retentionTime: Long = 0L,
    private val creationTs: Long = 0L,
    private val changes: Int = 0,
) : MegaChatRoom(0, false) {

    override fun delete() = Unit

    override fun getChatId(): Long = chatId
    override fun getOwnPrivilege(): Int = ownPrivilege
    override fun getNumPreviewers(): Long = 0L
    override fun getPeerPrivilegeByHandle(p0: Long): Int =
        peers.firstOrNull { it.first == p0 }?.second ?: MegaChatRoom.PRIV_UNKNOWN
    override fun getPeerCount(): Long = peers.size.toLong()
    override fun getPeerHandle(p0: Long): Long = peers.getOrNull(p0.toInt())?.first ?: -1L
    override fun getPeerPrivilege(p0: Long): Int =
        peers.getOrNull(p0.toInt())?.second ?: MegaChatRoom.PRIV_UNKNOWN
    override fun isGroup(): Boolean = isGroup
    override fun isPublic(): Boolean = isPublic
    override fun isNoteToSelf(): Boolean = isNoteToSelf
    override fun isPreview(): Boolean = isPreview
    override fun getAuthorizationToken(): String? = null
    override fun getTitle(): String = title
    override fun hasCustomTitle(): Boolean = false
    override fun getUnreadCount(): Int = unreadCount
    override fun getUserTyping(): Long = -1L
    override fun getUserHandle(): Long = -1L
    override fun isActive(): Boolean = isActive
    override fun isArchived(): Boolean = isArchived
    override fun getRetentionTime(): Long = retentionTime
    override fun getCreationTs(): Long = creationTs
    override fun isMeeting(): Boolean = isMeeting
    override fun isWaitingRoom(): Boolean = isWaitingRoom
    override fun isOpenInvite(): Boolean = isOpenInvite
    override fun isSpeakRequest(): Boolean = isSpeakRequest
    override fun getChanges(): Int = changes
    override fun hasChanged(p0: Int): Boolean = (changes and p0) != 0
}
