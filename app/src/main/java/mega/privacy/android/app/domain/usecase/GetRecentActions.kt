package mega.privacy.android.app.domain.usecase

import nz.mega.sdk.MegaRecentActionBucket

/**
 * Get a list of recent actions
 */
fun interface GetRecentActions {
    /**
     * Get a list of recent actions
     *
     * @return a list of recent actions
     */
    suspend operator fun invoke(): List<MegaRecentActionBucket>
}