package mega.privacy.android.data.test.stub

import nz.mega.sdk.MegaDateSection
import nz.mega.sdk.MegaDateSectionList

/**
 * In-memory stub of [MegaDateSectionList] that is safe to use in unit tests.
 *
 * Every public instance method is overridden, so no call ever reaches the native SDK
 * (which would crash the JVM given the null native pointer). Data getters are backed by
 * constructor parameters with sensible defaults; all other methods return benign constants.
 */
class StubMegaDateSectionList(
    sections: List<MegaDateSection> = emptyList(),
) : MegaDateSectionList(0, false) {

    private val items = sections.toMutableList()

    override fun delete() = Unit

    override fun get(p0: Int): MegaDateSection? = items.getOrNull(p0)
    override fun size(): Int = items.size
}
