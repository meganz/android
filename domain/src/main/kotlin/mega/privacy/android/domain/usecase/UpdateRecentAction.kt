package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.RecentActionBucket

/**
 * Update the recent action bucket given in parameter
 */
interface UpdateRecentAction {

    /**
     * Update the recent action bucket given in parameter
     *
     * @param currentBucket the current recent action bucket to update
     * @param cachedActionList the list of recent action bucket to compare with
     *
     * @return the update current bucket, null if the bucket does not exist anymore
     */
    suspend operator fun invoke(
        currentBucket: RecentActionBucket,
        cachedActionList: List<RecentActionBucket>?,
    ): RecentActionBucket?
}
