package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * A use case to monitor the update user data broadcast event
 */
class MonitorUpdateUserDataUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {

    /**
     * Monitor the update user data broadcast event from the repository
     */
    operator fun invoke() = accountRepository.monitorUpdateUserData()
}
