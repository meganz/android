package mega.privacy.android.data.test.stub

import nz.mega.sdk.MegaShare

/**
 * In-memory stub of [MegaShare] that is safe to use in unit tests.
 *
 * Every public instance method is overridden, so no call ever reaches the native SDK
 * (which would crash the JVM given the null native pointer). Data getters are backed by
 * constructor parameters with sensible defaults; all other methods return benign constants.
 */
class StubMegaShare(
    private val user: String? = null,
    private val nodeHandle: Long = -1L,
    private val access: Int = MegaShare.ACCESS_READ,
    private val timestamp: Long = 0L,
    private val isPending: Boolean = false,
    private val isVerified: Boolean = false,
) : MegaShare(0, false) {

    override fun delete() = Unit

    override fun getUser(): String? = user
    override fun getNodeHandle(): Long = nodeHandle
    override fun getAccess(): Int = access
    override fun getTimestamp(): Long = timestamp
    override fun isPending(): Boolean = isPending
    override fun isVerified(): Boolean = isVerified
}
