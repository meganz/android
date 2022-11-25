package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Default get extended account detail
 *
 */
class DefaultGetExtendedAccountDetail @Inject constructor(
    private val repository: AccountRepository,
) : GetExtendedAccountDetail {
    override suspend fun invoke(sessions: Boolean, purchases: Boolean, transactions: Boolean) {
        repository.resetExtendedAccountDetailsTimestamp()
        repository.getExtendedAccountDetails(sessions, purchases, transactions)
    }
}