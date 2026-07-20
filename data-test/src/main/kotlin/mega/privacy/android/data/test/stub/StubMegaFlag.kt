package mega.privacy.android.data.test.stub

import nz.mega.sdk.MegaFlag

/**
 * In-memory stub of [MegaFlag] that is safe to use in unit tests.
 *
 * Every public instance method is overridden, so no call ever reaches the native SDK
 * (which would crash the JVM given the null native pointer). Data getters are backed by
 * constructor parameters with sensible defaults; all other methods return benign constants.
 */
class StubMegaFlag(
    private val type: Long = 0L,
    private val group: Long = 0L,
) : MegaFlag(0, false) {

    override fun delete() = Unit

    override fun getType(): Long = type
    override fun getGroup(): Long = group
}
