package mega.privacy.android.data.mapper.recentactions

import nz.mega.sdk.MegaRecentActionBucket
import nz.mega.sdk.MegaRecentActionBucketList
import javax.inject.Inject

/**
 * Mapper to convert data into a list of [MegaRecentActionBucket].
 */
internal class RecentActionsMapper @Inject constructor() {

    /**
     * Invoke.
     *
     * @param recentActions [MegaRecentActionBucketList].
     * @param copyRecentActionBucket Provides the required copy of a [MegaRecentActionBucket].
     * @return List of [MegaRecentActionBucket].
     */
    suspend operator fun invoke(
        recentActions: MegaRecentActionBucketList?,
        copyRecentActionBucket: suspend (MegaRecentActionBucket) -> MegaRecentActionBucket,
    ) = recentActions?.let { actions ->
        (0 until actions.size())
            .filter { recentActions.get(it) != null }
            .map { copyRecentActionBucket(recentActions.get(it)) }
    } ?: emptyList()
}
