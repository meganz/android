package mega.privacy.android.data.test.stub

import nz.mega.sdk.MegaChatScheduledFlags
import nz.mega.sdk.MegaChatScheduledMeeting
import nz.mega.sdk.MegaChatScheduledRules

/**
 * In-memory stub of [MegaChatScheduledMeeting] that is safe to use in unit tests.
 *
 * Every public instance method is overridden, so no call ever reaches the native SDK
 * (which would crash the JVM given the null native pointer). Data getters are backed by
 * constructor parameters with sensible defaults; all other methods return benign constants.
 */
class StubMegaChatScheduledMeeting(
    private val chatId: Long = -1L,
    private val schedId: Long = -1L,
    private val parentSchedId: Long = -1L,
    private val organizerUserId: Long = -1L,
    private val timezone: String? = null,
    private val startDateTime: Long = 0L,
    private val endDateTime: Long = 0L,
    private val title: String? = null,
    private val description: String? = null,
    private val attributes: String? = null,
    private val overrides: Long = 0L,
    private val cancelled: Int = 0,
    private val flags: MegaChatScheduledFlags? = null,
    private val rules: MegaChatScheduledRules? = null,
    private val isNew: Boolean = false,
    private val isDeleted: Boolean = false,
) : MegaChatScheduledMeeting(0, false) {

    override fun delete() = Unit

    override fun cancelled(): Int = cancelled
    override fun hasChanged(p0: Long): Boolean = false
    override fun isNew(): Boolean = isNew
    override fun isDeleted(): Boolean = isDeleted
    override fun chatId(): Long = chatId
    override fun schedId(): Long = schedId
    override fun parentSchedId(): Long = parentSchedId
    override fun organizerUserId(): Long = organizerUserId
    override fun timezone(): String? = timezone
    override fun startDateTime(): Long = startDateTime
    override fun endDateTime(): Long = endDateTime
    override fun title(): String? = title
    override fun description(): String? = description
    override fun attributes(): String? = attributes
    override fun overrides(): Long = overrides
    override fun flags(): MegaChatScheduledFlags? = flags
    override fun rules(): MegaChatScheduledRules? = rules
}
