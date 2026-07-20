package mega.privacy.android.data.test.stub

import nz.mega.sdk.MegaRecentActionBucket
import nz.mega.sdk.MegaRecentActionBucketList

/**
 * In-memory stub of [MegaRecentActionBucketList] that is safe to use in unit tests.
 *
 * Every public instance method is overridden, so no call ever reaches the native SDK
 * (which would crash the JVM given the null native pointer). Data getters are backed by
 * constructor parameters with sensible defaults; all other methods return benign constants.
 */
class StubMegaRecentActionBucketList(
    buckets: List<MegaRecentActionBucket> = emptyList(),
) : MegaRecentActionBucketList(0, false) {

    private val items = buckets.toMutableList()

    override fun delete() = Unit

    override fun get(p0: Int): MegaRecentActionBucket? = items.getOrNull(p0)
    override fun size(): Int = items.size
}
