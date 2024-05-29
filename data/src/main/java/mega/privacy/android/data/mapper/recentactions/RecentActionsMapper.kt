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
     * @return List of [MegaRecentActionBucket].
     */
    operator fun invoke(
        recentActions: MegaRecentActionBucketList?,
    ) = recentActions?.let { actions ->
        (0 until actions.size())
            .mapNotNull { recentActions.get(it) }
    } ?: emptyList()
}
