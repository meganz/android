package mega.privacy.android.data.mapper.recentactions

import nz.mega.sdk.MegaRecentActionBucket
import nz.mega.sdk.MegaRecentActionBucketList
import javax.inject.Inject

/**
 * Implementation of [RecentActionsMapper].
 */
internal class RecentActionsMapperImpl @Inject constructor() : RecentActionsMapper {

    override suspend fun invoke(
        recentActions: MegaRecentActionBucketList?,
        copyRecentActionBucket: (MegaRecentActionBucket) -> MegaRecentActionBucket,
    ) = recentActions?.let { actions ->
        (0 until actions.size())
            .filter { recentActions.get(it) != null }
            .map { copyRecentActionBucket(recentActions.get(it)) }
    } ?: emptyList()
}