package mega.privacy.android.data.test.stub

import nz.mega.sdk.MegaContactRequest

/**
 * In-memory stub of [MegaContactRequest] that is safe to use in unit tests.
 *
 * Every public instance method is overridden, so no call ever reaches the native SDK
 * (which would crash the JVM given the null native pointer). Data getters are backed by
 * constructor parameters with sensible defaults; all other methods return benign constants.
 */
class StubMegaContactRequest(
    private val handle: Long = -1L,
    private val sourceEmail: String? = null,
    private val sourceMessage: String? = null,
    private val targetEmail: String? = null,
    private val creationTime: Long = 0L,
    private val modificationTime: Long = 0L,
    private val status: Int = MegaContactRequest.STATUS_UNRESOLVED,
    private val isOutgoing: Boolean = false,
    private val isAutoAccepted: Boolean = false,
) : MegaContactRequest(0, false) {

    override fun delete() = Unit

    override fun getHandle(): Long = handle
    override fun getSourceEmail(): String? = sourceEmail
    override fun getSourceMessage(): String? = sourceMessage
    override fun getTargetEmail(): String? = targetEmail
    override fun getCreationTime(): Long = creationTime
    override fun getModificationTime(): Long = modificationTime
    override fun getStatus(): Int = status
    override fun isOutgoing(): Boolean = isOutgoing
    override fun isAutoAccepted(): Boolean = isAutoAccepted
}
