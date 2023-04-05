package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus

/**
 * Business repository
 */
interface BusinessRepository {
    /**
     * Get business status
     *
     * @return current business account status
     */
    suspend fun getBusinessStatus(): BusinessAccountStatus

    /**
     * Checks whether the user's Business Account is currently active or not
     *
     * @return True if the user's Business Account is currently active, or
     * false if inactive or if the user is not under a Business Account
     */
    suspend fun isBusinessAccountActive(): Boolean
}