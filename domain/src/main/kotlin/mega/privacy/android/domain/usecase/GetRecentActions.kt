package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.RecentActionBucket

/**
 * Get a list of recent actions
 */
fun interface GetRecentActions {
    /**
     * Get a list of recent actions
     *
     * @return a list of recent actions
     */
    suspend operator fun invoke(): List<RecentActionBucket>
}
