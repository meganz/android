package mega.privacy.android.data.test.stub

import nz.mega.sdk.MegaChatContainsMeta
import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaChatScheduledRules
import nz.mega.sdk.MegaHandleList
import nz.mega.sdk.MegaNodeList
import nz.mega.sdk.MegaStringList
import nz.mega.sdk.MegaStringListMap

/**
 * In-memory stub of [MegaChatMessage] that is safe to use in unit tests.
 *
 * Every public instance method is overridden, so no call ever reaches the native SDK
 * (which would crash the JVM given the null native pointer). Data getters are backed by
 * constructor parameters with sensible defaults; all other methods return benign constants.
 */
class StubMegaChatMessage(
    private val msgId: Long = -1L,
    private val tempId: Long = -1L,
    private val msgIndex: Int = 0,
    private val userHandle: Long = -1L,
    private val type: Int = MegaChatMessage.TYPE_NORMAL,
    private val status: Int = MegaChatMessage.STATUS_UNKNOWN,
    private val timestamp: Long = 0L,
    private val content: String? = null,
    private val isEdited: Boolean = false,
    private val isDeleted: Boolean = false,
    private val isEditable: Boolean = false,
    private val isDeletable: Boolean = false,
    private val isManagementMessage: Boolean = false,
    private val handleOfAction: Long = -1L,
    private val privilege: Int = 0,
    private val code: Int = 0,
    private val usersCount: Long = 0L,
    private val duration: Int = 0,
    private val retentionTime: Long = 0L,
    private val termCode: Int = 0,
    private val rowId: Long = -1L,
    private val changes: Int = 0,
    private val megaNodeList: MegaNodeList? = null,
    private val megaHandleList: MegaHandleList? = null,
    private val containsMeta: MegaChatContainsMeta? = null,
) : MegaChatMessage(0, false) {

    override fun delete() = Unit

    override fun getStatus(): Int = status
    override fun getMsgId(): Long = msgId
    override fun getTempId(): Long = tempId
    override fun getMsgIndex(): Int = msgIndex
    override fun getUserHandle(): Long = userHandle
    override fun getType(): Int = type
    override fun hasConfirmedReactions(): Boolean = false
    override fun getTimestamp(): Long = timestamp
    override fun getContent(): String? = content
    override fun isEdited(): Boolean = isEdited
    override fun isDeleted(): Boolean = isDeleted
    override fun isEditable(): Boolean = isEditable
    override fun isDeletable(): Boolean = isDeletable
    override fun isNoteToSelf(): Boolean = false
    override fun isManagementMessage(): Boolean = isManagementMessage
    override fun getHandleOfAction(): Long = handleOfAction
    override fun getPrivilege(): Int = privilege
    override fun getCode(): Int = code
    override fun getUsersCount(): Long = usersCount
    override fun getUserHandle(p0: Long): Long = -1L
    override fun getUserName(p0: Long): String? = null
    override fun getUserEmail(p0: Long): String? = null
    override fun getMegaNodeList(): MegaNodeList? = megaNodeList
    override fun getMegaHandleList(): MegaHandleList? = megaHandleList
    override fun getDuration(): Int = duration
    override fun getRetentionTime(): Long = retentionTime
    override fun getTermCode(): Int = termCode
    override fun hasSchedMeetingChanged(p0: Long): Boolean = false
    override fun getStringList(): MegaStringList? = null
    override fun getStringListMap(): MegaStringListMap? = null
    override fun getScheduledMeetingChange(p0: Long): MegaStringList? = null
    override fun getScheduledMeetingRules(): MegaChatScheduledRules? = null
    override fun getRowId(): Long = rowId
    override fun getChanges(): Int = changes
    override fun hasChanged(p0: Int): Boolean = (changes and p0) != 0
    override fun getContainsMeta(): MegaChatContainsMeta? = containsMeta
}
