package mega.privacy.android.data.test.stub

import nz.mega.sdk.MegaChatError

/**
 * In-memory stub of [MegaChatError] that is safe to use in unit tests.
 *
 * Every public instance method is overridden, so no call ever reaches the native SDK
 * (which would crash the JVM given the null native pointer). Data getters are backed by
 * constructor parameters with sensible defaults; all other methods return benign constants.
 */
class StubMegaChatError(
    private val errorCode: Int = MegaChatError.ERROR_OK,
    private val errorString: String = "",
) : MegaChatError(0, false) {

    override fun delete() = Unit

    override fun getErrorCode(): Int = errorCode
    override fun getErrorType(): Int = 0
    override fun getErrorString(): String = errorString
    override fun toString(): String = errorString
}
