package mega.privacy.android.data.test.stub

import nz.mega.sdk.MegaFolderInfo

/**
 * In-memory stub of [MegaFolderInfo] that is safe to use in unit tests.
 *
 * Every public instance method is overridden, so no call ever reaches the native SDK
 * (which would crash the JVM given the null native pointer). Data getters are backed by
 * constructor parameters with sensible defaults.
 */
class StubMegaFolderInfo(
    private val numVersions: Int = 0,
    private val numFiles: Int = 0,
    private val numFolders: Int = 0,
    private val currentSize: Long = 0L,
    private val versionsSize: Long = 0L,
) : MegaFolderInfo(0, false) {

    override fun delete() = Unit

    override fun getNumVersions(): Int = numVersions
    override fun getNumFiles(): Int = numFiles
    override fun getNumFolders(): Int = numFolders
    override fun getCurrentSize(): Long = currentSize
    override fun getVersionsSize(): Long = versionsSize
}
