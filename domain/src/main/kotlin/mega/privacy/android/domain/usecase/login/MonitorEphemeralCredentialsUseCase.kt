package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Monitor ephemeral credentials use case
 *
 */
class MonitorEphemeralCredentialsUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    /**
     * Invoke
     *
     */
    operator fun invoke() = accountRepository.monitorEphemeralCredentials()
}