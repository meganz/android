package mega.privacy.android.data.test.stub

import nz.mega.sdk.MegaIntegerList
import nz.mega.sdk.MegaStringList
import nz.mega.sdk.MegaUserAlert

/**
 * In-memory stub of [MegaUserAlert] that is safe to use in unit tests.
 *
 * Every public instance method is overridden, so no call ever reaches the native SDK
 * (which would crash the JVM given the null native pointer). Data getters are backed by
 * constructor parameters with sensible defaults; all other methods return benign constants.
 */
class StubMegaUserAlert(
    private val id: Long = 0L,
    private val type: Int = 0,
    private val userHandle: Long = -1L,
    private val nodeHandle: Long = -1L,
    private val email: String? = null,
    private val heading: String? = null,
    private val title: String? = null,
    private val seen: Boolean = false,
    private val relevant: Boolean = true,
    private val timestamp: Long = 0L,
    private val number: Long = 0L,
    private val schedId: Long = -1L,
    private val pcrHandle: Long = -1L,
) : MegaUserAlert(0, false) {

    override fun delete() = Unit

    override fun getId(): Long = id
    override fun getSeen(): Boolean = seen
    override fun getRelevant(): Boolean = relevant
    override fun getType(): Int = type
    override fun getTypeString(): String = ""
    override fun getUserHandle(): Long = userHandle
    override fun getNodeHandle(): Long = nodeHandle
    override fun getPcrHandle(): Long = pcrHandle
    override fun getEmail(): String? = email
    override fun getPath(): String? = null
    override fun getName(): String? = null
    override fun getHeading(): String? = heading
    override fun getTitle(): String? = title
    override fun getNumber(p0: Long): Long = number
    override fun getTimestamp(p0: Long): Long = timestamp
    override fun getHandle(p0: Long): Long = -1L
    override fun getString(p0: Long): String? = null
    override fun getSchedId(): Long = schedId
    override fun hasSchedMeetingChanged(p0: Long): Boolean = false
    override fun getUpdatedTitle(): MegaStringList? = null
    override fun getUpdatedTimeZone(): MegaStringList? = null
    override fun getUpdatedStartDate(): MegaIntegerList? = null
    override fun getUpdatedEndDate(): MegaIntegerList? = null
    override fun isOwnChange(): Boolean = false
    override fun isRemoved(): Boolean = false
}
