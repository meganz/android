package mega.privacy.android.data.test.stub

import nz.mega.sdk.MegaStringList

/**
 * In-memory stub of [MegaStringList] that is safe to use in unit tests.
 *
 * Every public instance method is overridden, so no call ever reaches the native SDK
 * (which would crash the JVM given the null native pointer). Data getters are backed by
 * constructor parameters with sensible defaults; all other methods return benign constants.
 */
class StubMegaStringList(
    strings: List<String> = emptyList(),
) : MegaStringList(0, false) {

    private val items = strings.toMutableList()

    override fun delete() = Unit

    override fun get(p0: Int): String? = items.getOrNull(p0)
    override fun size(): Int = items.size
    override fun add(p0: String?) {
        p0?.let { items += it }
    }
}
