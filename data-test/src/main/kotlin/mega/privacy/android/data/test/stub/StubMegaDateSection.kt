package mega.privacy.android.data.test.stub

import nz.mega.sdk.MegaDateSection

/**
 * In-memory stub of [MegaDateSection] that is safe to use in unit tests.
 *
 * Every public instance method is overridden, so no call ever reaches the native SDK
 * (which would crash the JVM given the null native pointer). Data getters are backed by
 * constructor parameters with sensible defaults; all other methods return benign constants.
 */
class StubMegaDateSection(
    private val groupId: String = "",
    private val startDate: Long = 0L,
    private val endDate: Long = 0L,
    private val count: Long = 0L,
) : MegaDateSection(0, false) {

    override fun delete() = Unit

    override fun getGroupId(): String = groupId
    override fun getStartDate(): Long = startDate
    override fun getEndDate(): Long = endDate
    override fun getCount(): Long = count
}
