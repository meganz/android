package mega.privacy.android.data.test.stub

import nz.mega.sdk.MegaHandleList

/**
 * In-memory stub of [MegaHandleList] that is safe to use in unit tests.
 *
 * Every public instance method is overridden, so no call ever reaches the native SDK
 * (which would crash the JVM given the null native pointer). Data getters are backed by
 * constructor parameters with sensible defaults; all other methods return benign constants.
 */
class StubMegaHandleList(
    handles: List<Long> = emptyList(),
) : MegaHandleList(0, false) {

    private val items = handles.toMutableList()

    override fun delete() = Unit

    override fun get(p0: Long): Long = items.getOrElse(p0.toInt()) { -1L }
    override fun size(): Long = items.size.toLong()
    override fun addMegaHandle(p0: Long) {
        items += p0
    }
}
