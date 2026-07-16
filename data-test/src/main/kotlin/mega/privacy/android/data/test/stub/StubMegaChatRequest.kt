package mega.privacy.android.data.test.stub

import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaChatPeerList
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaChatScheduledMeetingList
import nz.mega.sdk.MegaChatScheduledMeetingOccurrList
import nz.mega.sdk.MegaHandleList
import nz.mega.sdk.MegaNodeList

/**
 * In-memory stub of [MegaChatRequest] that is safe to use in unit tests.
 *
 * Every public instance method is overridden, so no call ever reaches the native SDK
 * (which would crash the JVM given the null native pointer). Data getters are backed by
 * constructor parameters with sensible defaults; all other methods return benign constants.
 */
class StubMegaChatRequest(
    private val type: Int = 0,
    private val chatHandle: Long = -1L,
    private val userHandle: Long = -1L,
    private val privilege: Int = 0,
    private val text: String? = null,
    private val link: String? = null,
    private val flag: Boolean = false,
    private val number: Long = 0L,
    private val paramType: Int = 0,
    private val tag: Int = 0,
    private val megaChatPeerList: MegaChatPeerList? = null,
    private val megaChatMessage: MegaChatMessage? = null,
    private val megaNodeList: MegaNodeList? = null,
    private val megaHandleList: MegaHandleList? = null,
    private val megaChatScheduledMeetingList: MegaChatScheduledMeetingList? = null,
) : MegaChatRequest(0, false) {

    override fun delete() = Unit

    override fun getType(): Int = type
    override fun getRequestString(): String = ""
    override fun toString(): String = ""
    override fun getTag(): Int = tag
    override fun getNumber(): Long = number
    override fun getNumRetry(): Int = 0
    override fun getFlag(): Boolean = flag
    override fun getMegaChatPeerList(): MegaChatPeerList? = megaChatPeerList
    override fun getChatHandle(): Long = chatHandle
    override fun getUserHandle(): Long = userHandle
    override fun getPrivilege(): Int = privilege
    override fun getText(): String? = text
    override fun getLink(): String? = link
    override fun getMegaChatMessage(): MegaChatMessage? = megaChatMessage
    override fun getMegaNodeList(): MegaNodeList? = megaNodeList
    override fun getMegaHandleListByChat(p0: Long): MegaHandleList? = megaHandleList
    override fun getMegaChatScheduledMeetingList(): MegaChatScheduledMeetingList? =
        megaChatScheduledMeetingList
    override fun getMegaChatScheduledMeetingOccurrList(): MegaChatScheduledMeetingOccurrList? = null
    override fun getMegaHandleList(): MegaHandleList? = megaHandleList
    override fun getParamType(): Int = paramType
}
