package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * The use case for credentials whether is null
 */
class AreCredentialsNullUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {

    /**
     * Credentials whether is null
     *
     * @return true is null, otherwise is false
     */
    suspend operator fun invoke() = accountRepository.getAccountCredentials() == null
}