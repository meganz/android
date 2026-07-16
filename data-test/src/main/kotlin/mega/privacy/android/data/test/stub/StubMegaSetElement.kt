package mega.privacy.android.data.test.stub

import nz.mega.sdk.MegaSetElement

/**
 * In-memory stub of [MegaSetElement] that is safe to use in unit tests.
 *
 * Every public instance method is overridden, so no call ever reaches the native SDK
 * (which would crash the JVM given the null native pointer). Data getters are backed by
 * constructor parameters with sensible defaults; all other methods return benign constants.
 */
class StubMegaSetElement(
    private val id: Long = -1L,
    private val node: Long = -1L,
    private val setId: Long = -1L,
    private val order: Long = 0L,
    private val ts: Long = 0L,
    private val name: String = "",
    private val changes: Long = 0L,
) : MegaSetElement(0, false) {

    override fun delete() = Unit

    override fun id(): Long = id
    override fun node(): Long = node
    override fun setId(): Long = setId
    override fun order(): Long = order
    override fun ts(): Long = ts
    override fun name(): String = name
    override fun hasChanged(p0: Long): Boolean = (changes and p0) != 0L
    override fun getChanges(): Long = changes
}
