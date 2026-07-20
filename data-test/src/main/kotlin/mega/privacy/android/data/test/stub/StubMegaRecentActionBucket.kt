package mega.privacy.android.data.test.stub

import nz.mega.sdk.MegaNodeList
import nz.mega.sdk.MegaRecentActionBucket

/**
 * In-memory stub of [MegaRecentActionBucket] that is safe to use in unit tests.
 *
 * Every public instance method is overridden, so no call ever reaches the native SDK
 * (which would crash the JVM given the null native pointer). Data getters are backed by
 * constructor parameters with sensible defaults; all other methods return benign constants.
 */
class StubMegaRecentActionBucket(
    private val timestamp: Long = 0L,
    private val userEmail: String? = null,
    private val parentHandle: Long = -1L,
    private val isUpdate: Boolean = false,
    private val isMedia: Boolean = false,
    private val id: String? = null,
    private val nodes: MegaNodeList = StubMegaNodeList(),
) : MegaRecentActionBucket(0, false) {

    override fun delete() = Unit

    override fun getTimestamp(): Long = timestamp
    override fun getUserEmail(): String? = userEmail
    override fun getParentHandle(): Long = parentHandle
    override fun isUpdate(): Boolean = isUpdate
    override fun isMedia(): Boolean = isMedia
    override fun getId(): String? = id
    override fun getNodes(): MegaNodeList = nodes
}
