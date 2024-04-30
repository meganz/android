package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use case to check if credentials exist
 *
 */
class HasCredentialsUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {

    /**
     * Invocation function
     *
     * @return do credentials exist
     */
    suspend operator fun invoke(): Boolean = accountRepository.getAccountCredentials() != null
}
