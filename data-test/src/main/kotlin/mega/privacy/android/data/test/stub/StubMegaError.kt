package mega.privacy.android.data.test.stub

import nz.mega.sdk.MegaError

/**
 * In-memory stub of [MegaError] that is safe to use in unit tests.
 *
 * Every public instance method is overridden, so no call ever reaches the native SDK
 * (which would crash the JVM given the null native pointer). Data getters are backed by
 * constructor parameters with sensible defaults; all other methods return benign constants.
 */
class StubMegaError(
    private val errorCode: Int = MegaError.API_OK,
    private val errorString: String = "",
) : MegaError(0, false) {

    override fun delete() = Unit

    override fun getErrorCode(): Int = errorCode
    override fun getMountResult(): Int = 0
    override fun getSyncError(): Int = 0
    override fun getValue(): Long = 0L
    override fun hasExtraInfo(): Boolean = false
    override fun getUserStatus(): Long = 0L
    override fun getLinkStatus(): Long = 0L
    override fun getErrorString(): String = errorString
    override fun toString(): String = errorString
}
