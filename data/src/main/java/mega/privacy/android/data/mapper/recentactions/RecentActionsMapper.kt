package mega.privacy.android.data.mapper.recentactions

import nz.mega.sdk.MegaRecentActionBucket
import nz.mega.sdk.MegaRecentActionBucketList

/**
 * Mapper to convert data into a list of [MegaRecentActionBucket].
 */
internal fun interface RecentActionsMapper {

    /**
     * Invoke.
     *
     * @param recentActions [MegaRecentActionBucketList].
     * @param copyRecentActionBucket Provides the required copy of a [MegaRecentActionBucket].
     * @return List of [MegaRecentActionBucket].
     */
    suspend operator fun invoke(
        recentActions: MegaRecentActionBucketList?,
        copyRecentActionBucket: (MegaRecentActionBucket) -> MegaRecentActionBucket,
    ): List<MegaRecentActionBucket>
}