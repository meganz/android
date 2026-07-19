package mega.privacy.android.data.test.stub

import nz.mega.sdk.MegaSet

/**
 * In-memory stub of [MegaSet] that is safe to use in unit tests.
 *
 * Every public instance method is overridden, so no call ever reaches the native SDK
 * (which would crash the JVM given the null native pointer). Data getters are backed by
 * constructor parameters with sensible defaults; all other methods return benign constants.
 */
class StubMegaSet(
    private val id: Long = -1L,
    private val publicId: Long = -1L,
    private val user: Long = -1L,
    private val ts: Long = 0L,
    private val cts: Long = 0L,
    private val type: Int = MegaSet.SET_TYPE_ALBUM,
    private val name: String = "",
    private val cover: Long = -1L,
    private val changes: Long = 0L,
    private val isExported: Boolean = false,
    private val isTakenDown: Boolean = false,
) : MegaSet(0, false) {

    override fun delete() = Unit

    override fun id(): Long = id
    override fun publicId(): Long = publicId
    override fun user(): Long = user
    override fun ts(): Long = ts
    override fun cts(): Long = cts
    override fun type(): Int = type
    override fun name(): String = name
    override fun cover(): Long = cover
    override fun hasChanged(p0: Long): Boolean = (changes and p0) != 0L
    override fun getChanges(): Long = changes
    override fun isExported(): Boolean = isExported
    override fun getLinkDeletionReason(): Int = 0
    override fun isTakenDown(): Boolean = isTakenDown
}
