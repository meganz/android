package mega.privacy.android.data.repository

import nz.mega.sdk.MegaRecentActionBucket

/**
 * Recent actions repository
 */
interface RecentActionsRepository {

    /**
     * Gets the recent actions.
     *
     * The recommended values for days and maxNodes parameters are to consider
     * interactions during the last 30 days and maximum 500 nodes. So they are set by default.
     *
     * @return a list of recent actions.
     */
    suspend fun getRecentActions(): List<MegaRecentActionBucket>
}