package mega.privacy.android.data.test.stub

import nz.mega.sdk.MegaFileServiceReclaimOptions
import nz.mega.sdk.SWIGTYPE_p_std__size_t

/**
 * In-memory stub of [MegaFileServiceReclaimOptions] that is safe to use in unit tests.
 *
 * Every public instance method is overridden, so no call ever reaches the native SDK
 * (which would crash the JVM given the null native pointer). Data getters are backed by
 * constructor parameters with sensible defaults; all other methods return benign constants.
 */
class StubMegaFileServiceReclaimOptions(
    private var ageThreshold: Int = 0,
    private var delay: Long = 0L,
    private var period: Long = 0L,
    private var reclaimThreshold: Long = 0L,
    private var reclaimTarget: Long = 0L,
) : MegaFileServiceReclaimOptions(0, false) {

    override fun delete() = Unit

    override fun getAgeThreshold(): Int = ageThreshold
    override fun setAgeThreshold(p0: Int) {
        ageThreshold = p0
    }
    override fun getBatchSize(): SWIGTYPE_p_std__size_t? = null
    override fun setBatchSize(p0: SWIGTYPE_p_std__size_t?) = Unit
    override fun getDelay(): Long = delay
    override fun setDelay(p0: Long) {
        delay = p0
    }
    override fun getPeriod(): Long = period
    override fun setPeriod(p0: Long) {
        period = p0
    }
    override fun getReclaimThreshold(): Long = reclaimThreshold
    override fun setReclaimThreshold(p0: Long) {
        reclaimThreshold = p0
    }
    override fun getReclaimTarget(): Long = reclaimTarget
    override fun setReclaimTarget(p0: Long) {
        reclaimTarget = p0
    }
}
