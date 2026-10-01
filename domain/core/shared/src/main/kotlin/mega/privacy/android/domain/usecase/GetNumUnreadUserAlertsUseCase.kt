package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use case for getting the number of unread user alerts for the logged in user.
 */
class GetNumUnreadUserAlertsUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {

    /**
     * Invoke.
     *
     * @return Number of unread user alerts.
     */
    suspend operator fun invoke() = accountRepository.getNumUnreadUserAlerts()
}