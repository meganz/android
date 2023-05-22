package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Clear ephemeral credentials use case
 *
 */
class ClearEphemeralCredentialsUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke() =
        accountRepository.clearEphemeral()
}