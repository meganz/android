package mega.privacy.android.data.test.stub

import nz.mega.sdk.MegaChatScheduledRules
import nz.mega.sdk.MegaIntegerList
import nz.mega.sdk.MegaIntegerMap

/**
 * In-memory stub of [MegaChatScheduledRules] that is safe to use in unit tests.
 *
 * Every public instance method is overridden, so no call ever reaches the native SDK
 * (which would crash the JVM given the null native pointer). Data getters are backed by
 * constructor parameters with sensible defaults; all other methods return benign constants.
 */
class StubMegaChatScheduledRules(
    private var freq: Int = MegaChatScheduledRules.FREQ_INVALID,
    private var interval: Int = MegaChatScheduledRules.INTERVAL_INVALID,
    private var until: Long = 0L,
    private var byWeekDay: MegaIntegerList? = null,
    private var byMonthDay: MegaIntegerList? = null,
    private var byMonthWeekDay: MegaIntegerMap? = null,
) : MegaChatScheduledRules(0, false) {

    override fun delete() = Unit

    override fun setFreq(p0: Int) {
        freq = p0
    }
    override fun setInterval(p0: Int) {
        interval = p0
    }
    override fun setUntil(p0: Long) {
        until = p0
    }
    override fun setByWeekDay(p0: MegaIntegerList?) {
        byWeekDay = p0
    }
    override fun setByMonthDay(p0: MegaIntegerList?) {
        byMonthDay = p0
    }
    override fun setByMonthWeekDay(p0: MegaIntegerMap?) {
        byMonthWeekDay = p0
    }
    override fun freq(): Int = freq
    override fun interval(): Int = interval
    override fun until(): Long = until
    override fun byWeekDay(): MegaIntegerList? = byWeekDay
    override fun byMonthDay(): MegaIntegerList? = byMonthDay
    override fun byMonthWeekDay(): MegaIntegerMap? = byMonthWeekDay
}
