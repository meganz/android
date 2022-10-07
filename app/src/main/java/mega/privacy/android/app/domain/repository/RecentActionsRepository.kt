package mega.privacy.android.app.domain.repository

import nz.mega.sdk.MegaRecentActionBucket

/**
 * Recent actions repository
 */
interface RecentActionsRepository {

    /**
     * Get the recent actions
     *
     * @return a list of recent actions
     */
    suspend fun getRecentActions(): List<MegaRecentActionBucket>
}