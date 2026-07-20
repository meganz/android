package mega.privacy.android.data.test.stub

import nz.mega.sdk.MegaSync
import nz.mega.sdk.MegaSyncList

/**
 * In-memory stub of [MegaSyncList] that is safe to use in unit tests.
 *
 * Every public instance method is overridden, so no call ever reaches the native SDK
 * (which would crash the JVM given the null native pointer). Data getters are backed by
 * constructor parameters with sensible defaults; all other methods return benign constants.
 */
class StubMegaSyncList(
    syncs: List<MegaSync> = emptyList(),
) : MegaSyncList(0, false) {

    private val items = syncs.toMutableList()

    override fun delete() = Unit

    override fun get(p0: Int): MegaSync? = items.getOrNull(p0)
    override fun size(): Int = items.size
    override fun addSync(p0: MegaSync?) {
        p0?.let { items += it }
    }
}
