package mega.privacy.android.data.test.stub

import nz.mega.sdk.MegaChatPeerList

/**
 * In-memory stub of [MegaChatPeerList] that is safe to use in unit tests.
 *
 * Every public instance method is overridden, so no call ever reaches the native SDK
 * (which would crash the JVM given the null native pointer). Data getters are backed by
 * constructor parameters with sensible defaults; all other methods return benign constants.
 */
class StubMegaChatPeerList(
    peers: List<Pair<Long, Int>> = emptyList(),
) : MegaChatPeerList(0, false) {

    private val items = peers.toMutableList()

    override fun delete() = Unit

    override fun addPeer(p0: Long, p1: Int) {
        items += p0 to p1
    }
    override fun getPeerHandle(p0: Int): Long = items.getOrNull(p0)?.first ?: -1L
    override fun getPeerPrivilege(p0: Int): Int =
        items.getOrNull(p0)?.second ?: MegaChatPeerList.PRIV_UNKNOWN
    override fun size(): Int = items.size
}
