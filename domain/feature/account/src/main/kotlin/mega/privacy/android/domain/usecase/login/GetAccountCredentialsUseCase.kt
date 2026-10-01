package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.entity.user.UserCredentials
import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use case for getting the credentials of the current logged in account.
 */
class GetAccountCredentialsUseCase @Inject constructor(private val accountRepository: AccountRepository) {

    /**
     * Invoke.
     *
     * @return [UserCredentials]
     */
    suspend operator fun invoke() = accountRepository.getAccountCredentials()
}