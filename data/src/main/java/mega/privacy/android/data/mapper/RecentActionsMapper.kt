package mega.privacy.android.data.mapper

import nz.mega.sdk.MegaRecentActionBucket
import nz.mega.sdk.MegaRecentActionBucketList

/**
 * Map [RecentActionsMapper] to list of [MegaRecentActionBucket]
 */
typealias RecentActionsMapper = @JvmSuppressWildcards suspend (
    @JvmSuppressWildcards MegaRecentActionBucketList?,
    @JvmSuppressWildcards MegaRecentActionBucketProvider,
) -> @JvmSuppressWildcards List<MegaRecentActionBucket>

/**
 * Provide the [MegaRecentActionBucket] required copy.
 */
typealias MegaRecentActionBucketProvider = suspend (@JvmSuppressWildcards MegaRecentActionBucket) -> @JvmSuppressWildcards MegaRecentActionBucket

internal suspend fun toRecentActionBucketList(
    recentActions: MegaRecentActionBucketList?,
    recentActionBucketProvider: MegaRecentActionBucketProvider,
): List<MegaRecentActionBucket> =
    recentActions?.let { actions ->
        (0 until actions.size())
            .filter { recentActions.get(it) != null }
            .map { recentActionBucketProvider(recentActions.get(it)) }
    } ?: emptyList()