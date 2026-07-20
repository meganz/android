package mega.privacy.android.data.test.stub

import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaChatPresenceConfig

/**
 * In-memory stub of [MegaChatPresenceConfig] that is safe to use in unit tests.
 *
 * Every public instance method is overridden, so no call ever reaches the native SDK
 * (which would crash the JVM given the null native pointer). Data getters are backed by
 * constructor parameters with sensible defaults; all other methods return benign constants.
 */
class StubMegaChatPresenceConfig(
    private val onlineStatus: Int = MegaChatApi.STATUS_ONLINE,
    private val isAutoawayEnabled: Boolean = false,
    private val autoawayTimeout: Long = 0L,
    private val isPersist: Boolean = false,
    private val isPending: Boolean = false,
    private val isLastGreenVisible: Boolean = false,
) : MegaChatPresenceConfig(0, false) {

    override fun delete() = Unit

    override fun getOnlineStatus(): Int = onlineStatus
    override fun isAutoawayEnabled(): Boolean = isAutoawayEnabled
    override fun getAutoawayTimeout(): Long = autoawayTimeout
    override fun isPersist(): Boolean = isPersist
    override fun isPending(): Boolean = isPending
    override fun isLastGreenVisible(): Boolean = isLastGreenVisible
}
