package mega.privacy.android.data.mapper

import nz.mega.sdk.MegaRecentActionBucket
import nz.mega.sdk.MegaRecentActionBucketList

/**
 * Map [RecentActionsMapper] to list of [MegaRecentActionBucket]
 */
typealias RecentActionsMapper = @JvmSuppressWildcards (@JvmSuppressWildcards MegaRecentActionBucketList?, @JvmSuppressWildcards MegaRecentActionBucketProvider) -> @JvmSuppressWildcards List<MegaRecentActionBucket>

/**
 * Provide the [MegaRecentActionBucket] required copy.
 */
typealias MegaRecentActionBucketProvider = (@JvmSuppressWildcards MegaRecentActionBucket) -> @JvmSuppressWildcards MegaRecentActionBucket

internal fun toRecentActionBucketList(
    recentActions: MegaRecentActionBucketList?,
    recentActionBucketProvider: MegaRecentActionBucketProvider,
): List<MegaRecentActionBucket> =
    recentActions?.let { actions ->
        (0 until actions.size())
            .filterNotNull()
            .map { recentActionBucketProvider(recentActions.get(it)) }
    } ?: emptyList()