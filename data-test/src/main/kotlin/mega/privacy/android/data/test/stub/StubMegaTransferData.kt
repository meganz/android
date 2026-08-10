package mega.privacy.android.data.test.stub

import nz.mega.sdk.MegaTransferData

/**
 * In-memory stub of [MegaTransferData] that is safe to use in unit tests.
 *
 * Every public instance method is overridden, so no call ever reaches the native SDK
 * (which would crash the JVM given the null native pointer). Data getters are backed by
 * constructor parameters with sensible defaults; all other methods return benign constants.
 */
class StubMegaTransferData(
    private val numDownloads: Int = 0,
    private val numUploads: Int = 0,
    private val notificationNumber: Long = 0L,
) : MegaTransferData(0, false) {

    override fun delete() = Unit

    override fun getNumDownloads(): Int = numDownloads
    override fun getNumUploads(): Int = numUploads
    override fun getDownloadTag(p0: Int): Int = 0
    override fun getUploadTag(p0: Int): Int = 0
    override fun getDownloadPriority(p0: Int): java.math.BigInteger = java.math.BigInteger.ZERO
    override fun getUploadPriority(p0: Int): java.math.BigInteger = java.math.BigInteger.ZERO
    override fun getNotificationNumber(): Long = notificationNumber
}
