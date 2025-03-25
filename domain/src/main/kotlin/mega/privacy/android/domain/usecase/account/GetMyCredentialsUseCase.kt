package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use Case to retrieve the Account credentials
 */
class GetMyCredentialsUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {

    /**
     * Invocation function
     *
     * @return the User credentials
     */
    suspend operator fun invoke() = accountRepository.getMyCredentials()
}