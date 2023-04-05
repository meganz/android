package mega.privacy.android.data.mapper.account.business

import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import javax.inject.Inject

/**
 * Business account status mapper
 */
internal class BusinessAccountStatusMapper @Inject constructor() {
    /**
     * Invoke
     *
     * @param accountStatusInteger
     * @return BusinessAccountStatus
     */
    internal operator fun invoke(accountStatusInteger: Int): BusinessAccountStatus =
        when (accountStatusInteger) {
            -1 -> BusinessAccountStatus.Expired
            0 -> BusinessAccountStatus.Inactive
            1 -> BusinessAccountStatus.Active
            2 -> BusinessAccountStatus.GracePeriod
            else -> throw IllegalArgumentException("Business account status of $accountStatusInteger is not a valid value.")
        }
}