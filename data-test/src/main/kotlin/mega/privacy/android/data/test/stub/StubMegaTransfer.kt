package mega.privacy.android.data.test.stub

import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaTransfer

/**
 * In-memory stub of [MegaTransfer] that is safe to use in unit tests.
 *
 * Every public instance method is overridden, so no call ever reaches the native SDK
 * (which would crash the JVM given the null native pointer). Data getters are backed by
 * constructor parameters with sensible defaults; all other methods return benign constants.
 */
class StubMegaTransfer(
    private val type: Int = MegaTransfer.TYPE_DOWNLOAD,
    private val tag: Int = 0,
    private val uniqueId: Long = 0L,
    private val fileName: String? = null,
    private val path: String? = null,
    private val parentPath: String? = null,
    private val nodeHandle: Long = -1L,
    private val parentHandle: Long = -1L,
    private val transferredBytes: Long = 0L,
    private val totalBytes: Long = 0L,
    private val startTime: Long = 0L,
    private val speed: Long = 0L,
    private val state: Int = MegaTransfer.STATE_NONE,
    private val priority: java.math.BigInteger = java.math.BigInteger.ZERO,
    private val stage: Long = 0L,
    private val isFolderTransfer: Boolean = false,
    private val isStreamingTransfer: Boolean = false,
    private val isFinished: Boolean = false,
    private val isSyncTransfer: Boolean = false,
    private val isBackupTransfer: Boolean = false,
    private val appData: String? = null,
    private val notificationNumber: Long = 0L,
    private val folderTransferTag: Int = 0,
    private val lastErrorExtended: MegaError? = null,
) : MegaTransfer(0, false) {

    override fun delete() = Unit

    override fun getType(): Int = type
    override fun getTransferString(): String = ""
    override fun toString(): String = ""
    override fun getStartTime(): Long = startTime
    override fun getTransferredBytes(): Long = transferredBytes
    override fun getTotalBytes(): Long = totalBytes
    override fun getPath(): String? = path
    override fun getParentPath(): String? = parentPath
    override fun getNodeHandle(): Long = nodeHandle
    override fun getParentHandle(): Long = parentHandle
    override fun getStartPos(): Long = 0L
    override fun getEndPos(): Long = 0L
    override fun getFileName(): String? = fileName
    override fun getNumRetry(): Int = 0
    override fun getMaxRetries(): Int = 0
    override fun getStage(): Long = stage
    override fun getUniqueId(): Long = uniqueId
    override fun getTag(): Int = tag
    override fun getSpeed(): Long = speed
    override fun getMeanSpeed(): Long = 0L
    override fun getDeltaSize(): Long = 0L
    override fun getUpdateTime(): Long = 0L
    override fun getPublicMegaNode(): MegaNode? = null
    override fun isSyncTransfer(): Boolean = isSyncTransfer
    override fun isBackupTransfer(): Boolean = isBackupTransfer
    override fun isForeignOverquota(): Boolean = false
    override fun isForceNewUpload(): Boolean = false
    override fun isStreamingTransfer(): Boolean = isStreamingTransfer
    override fun isFinished(): Boolean = isFinished
    override fun getLastBytes(): String? = null
    override fun getLastErrorExtended(): MegaError? = lastErrorExtended
    override fun isFolderTransfer(): Boolean = isFolderTransfer
    override fun getFolderTransferTag(): Int = folderTransferTag
    override fun getAppData(): String? = appData
    override fun getState(): Int = state
    override fun getPriority(): java.math.BigInteger = priority
    override fun getNotificationNumber(): Long = notificationNumber
    override fun getTargetOverride(): Boolean = false
    override fun getCancelToken(): MegaCancelToken? = null
}
