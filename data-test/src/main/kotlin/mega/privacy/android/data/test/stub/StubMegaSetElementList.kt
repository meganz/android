package mega.privacy.android.data.test.stub

import nz.mega.sdk.MegaSetElement
import nz.mega.sdk.MegaSetElementList

/**
 * In-memory stub of [MegaSetElementList] that is safe to use in unit tests.
 *
 * Every public instance method is overridden, so no call ever reaches the native SDK
 * (which would crash the JVM given the null native pointer). Data getters are backed by
 * constructor parameters with sensible defaults; all other methods return benign constants.
 */
class StubMegaSetElementList(
    elements: List<MegaSetElement> = emptyList(),
) : MegaSetElementList(0, false) {

    private val items = elements.toMutableList()

    override fun delete() = Unit

    override fun get(p0: Long): MegaSetElement? = items.getOrNull(p0.toInt())
    override fun size(): Long = items.size.toLong()
}
