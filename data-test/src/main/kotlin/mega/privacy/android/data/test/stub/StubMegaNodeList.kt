package mega.privacy.android.data.test.stub

import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaNodeList

/**
 * In-memory stub of [MegaNodeList] that is safe to use in unit tests.
 *
 * Every public instance method is overridden, so no call ever reaches the native SDK
 * (which would crash the JVM given the null native pointer). Data getters are backed by
 * constructor parameters with sensible defaults; all other methods return benign constants.
 */
class StubMegaNodeList(
    nodes: List<MegaNode> = emptyList(),
) : MegaNodeList(0, false) {

    private val items = nodes.toMutableList()

    override fun delete() = Unit

    override fun get(p0: Int): MegaNode? = items.getOrNull(p0)
    override fun size(): Int = items.size
    override fun addNode(p0: MegaNode?) {
        p0?.let { items += it }
    }
}
