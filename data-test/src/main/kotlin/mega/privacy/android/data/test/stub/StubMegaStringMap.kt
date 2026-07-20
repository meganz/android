package mega.privacy.android.data.test.stub

import nz.mega.sdk.MegaStringList
import nz.mega.sdk.MegaStringMap

/**
 * In-memory stub of [MegaStringMap] that is safe to use in unit tests.
 *
 * Every public instance method is overridden, so no call ever reaches the native SDK
 * (which would crash the JVM given the null native pointer). Data getters are backed by
 * constructor parameters with sensible defaults; all other methods return benign constants.
 */
class StubMegaStringMap(
    entries: Map<String, String> = emptyMap(),
) : MegaStringMap(0, false) {

    private val map = entries.toMutableMap()

    override fun delete() = Unit

    override fun get(p0: String?): String? = map[p0]
    override fun getKeys(): MegaStringList = StubMegaStringList(map.keys.toList())
    override fun set(p0: String?, p1: String?) {
        if (p0 != null && p1 != null) map[p0] = p1
    }
    override fun size(): Int = map.size
}
