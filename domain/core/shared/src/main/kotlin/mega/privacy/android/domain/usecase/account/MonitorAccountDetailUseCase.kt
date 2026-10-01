package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Monitor account detail
 *
 */
class MonitorAccountDetailUseCase @Inject constructor(
    private val repository: AccountRepository
) {
    /**
     * Invoke
     *
     * @return [Flow<AccountDetail>]
     */
    operator fun invoke() = repository.monitorAccountDetail()
}