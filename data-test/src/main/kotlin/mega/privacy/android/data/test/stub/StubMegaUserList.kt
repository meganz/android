package mega.privacy.android.data.test.stub

import nz.mega.sdk.MegaUser
import nz.mega.sdk.MegaUserList

/**
 * In-memory stub of [MegaUserList] that is safe to use in unit tests.
 *
 * Every public instance method is overridden, so no call ever reaches the native SDK
 * (which would crash the JVM given the null native pointer). Data getters are backed by
 * constructor parameters with sensible defaults; all other methods return benign constants.
 */
class StubMegaUserList(
    users: List<MegaUser> = emptyList(),
) : MegaUserList(0, false) {

    private val items = users.toMutableList()

    override fun delete() = Unit

    override fun get(p0: Int): MegaUser? = items.getOrNull(p0)
    override fun size(): Int = items.size
}
