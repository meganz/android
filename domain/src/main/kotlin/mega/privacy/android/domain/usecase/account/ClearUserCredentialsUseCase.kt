package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Clear user credentials use case
 *
 */
class ClearUserCredentialsUseCase @Inject constructor(
    private val repository: AccountRepository,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke() = repository.clearCredentials()
}