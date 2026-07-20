package mega.privacy.android.data.test.stub

import nz.mega.sdk.MegaUser

/**
 * In-memory stub of [MegaUser] that is safe to use in unit tests.
 *
 * Every public instance method is overridden, so no call ever reaches the native SDK
 * (which would crash the JVM given the null native pointer). Data getters are backed by
 * constructor parameters with sensible defaults; all other methods return benign constants.
 */
class StubMegaUser(
    private val email: String = "",
    private val handle: Long = -1L,
    private val visibility: Int = MegaUser.VISIBILITY_VISIBLE,
    private val timestamp: Long = 0L,
    private val changes: Long = 0L,
) : MegaUser(0, false) {

    override fun delete() = Unit

    override fun getEmail(): String = email
    override fun getHandle(): Long = handle
    override fun getVisibility(): Int = visibility
    override fun getTimestamp(): Long = timestamp
    override fun hasChanged(p0: Long): Boolean = (changes and p0) != 0L
    override fun getChanges(): Long = changes
    override fun isOwnChange(): Int = 0
}
