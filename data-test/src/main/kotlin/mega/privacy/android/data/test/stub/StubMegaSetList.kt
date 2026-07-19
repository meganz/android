package mega.privacy.android.data.test.stub

import nz.mega.sdk.MegaSet
import nz.mega.sdk.MegaSetList

/**
 * In-memory stub of [MegaSetList] that is safe to use in unit tests.
 *
 * Every public instance method is overridden, so no call ever reaches the native SDK
 * (which would crash the JVM given the null native pointer). Data getters are backed by
 * constructor parameters with sensible defaults; all other methods return benign constants.
 */
class StubMegaSetList(
    sets: List<MegaSet> = emptyList(),
) : MegaSetList(0, false) {

    private val items = sets.toMutableList()

    override fun delete() = Unit

    override fun get(p0: Long): MegaSet? = items.getOrNull(p0.toInt())
    override fun size(): Long = items.size.toLong()
}
