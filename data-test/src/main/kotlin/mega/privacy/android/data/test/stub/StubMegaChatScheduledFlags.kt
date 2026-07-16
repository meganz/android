package mega.privacy.android.data.test.stub

import nz.mega.sdk.MegaChatScheduledFlags

/**
 * In-memory stub of [MegaChatScheduledFlags] that is safe to use in unit tests.
 *
 * Every public instance method is overridden, so no call ever reaches the native SDK
 * (which would crash the JVM given the null native pointer). Data getters are backed by
 * constructor parameters with sensible defaults; all other methods return benign constants.
 */
class StubMegaChatScheduledFlags(
    private var sendEmails: Boolean = false,
) : MegaChatScheduledFlags(0, false) {

    override fun delete() = Unit

    override fun reset() {
        sendEmails = false
    }
    override fun setSendEmails(p0: Boolean) {
        sendEmails = p0
    }
    override fun sendEmails(): Boolean = sendEmails
    override fun isEmpty(): Boolean = !sendEmails
}
