package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Monitor user credentials use case
 *
 */
class MonitorUserCredentialsUseCase @Inject constructor(
    private val repository: AccountRepository,
) {
    /**
     * Invoke
     *
     */
    operator fun invoke() =
        repository.monitorCredentials()
}