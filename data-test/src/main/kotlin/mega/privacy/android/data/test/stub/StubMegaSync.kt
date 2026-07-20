package mega.privacy.android.data.test.stub

import nz.mega.sdk.MegaSync

/**
 * In-memory stub of [MegaSync] that is safe to use in unit tests.
 *
 * Every public instance method is overridden, so no call ever reaches the native SDK
 * (which would crash the JVM given the null native pointer). Data getters are backed by
 * constructor parameters with sensible defaults; all other methods return benign constants.
 */
class StubMegaSync(
    private val megaHandle: Long = -1L,
    private val localFolder: String? = null,
    private val name: String? = null,
    private val lastKnownMegaFolder: String? = null,
    private val backupId: Long = -1L,
    private val error: Int = 0,
    private val warning: Int = 0,
    private val type: Int = 0,
    private val runState: Int = 0,
) : MegaSync(0, false) {

    override fun delete() = Unit

    override fun getMegaHandle(): Long = megaHandle
    override fun getLocalFolder(): String? = localFolder
    override fun getName(): String? = name
    override fun getLastKnownMegaFolder(): String? = lastKnownMegaFolder
    override fun getBackupId(): Long = backupId
    override fun getError(): Int = error
    override fun getWarning(): Int = warning
    override fun getType(): Int = type
    override fun getRunState(): Int = runState
    override fun getMegaSyncErrorCode(): String = ""
    override fun getMegaSyncWarningCode(): String = ""
}
