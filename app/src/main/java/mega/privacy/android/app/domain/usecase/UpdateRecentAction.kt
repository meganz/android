package mega.privacy.android.app.domain.usecase

import nz.mega.sdk.MegaRecentActionBucket

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
        currentBucket: MegaRecentActionBucket,
        cachedActionList: List<MegaRecentActionBucket>?,
    ): MegaRecentActionBucket?
}