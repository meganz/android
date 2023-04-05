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
}