package mega.privacy.android.data.test.stub

import nz.mega.sdk.MegaCancelToken

/**
 * In-memory stub of [MegaCancelToken] that is safe to use in unit tests.
 *
 * Every public instance method is overridden, so no call ever reaches the native SDK
 * (which would crash the JVM given the null native pointer). Data getters are backed by
 * constructor parameters with sensible defaults; all other methods return benign constants.
 */
class StubMegaCancelToken(
    cancelled: Boolean = false,
) : MegaCancelToken(0, false) {

    private var cancelledFlag = cancelled

    override fun delete() = Unit

    override fun cancel() {
        cancelledFlag = true
    }
    override fun isCancelled(): Boolean = cancelledFlag
}
