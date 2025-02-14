package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.entity.login.EphemeralCredentials
import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use case for creating an account.
 */
class CreateAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {

    /**
     * Invoke
     *
     * @param email User's email to register
     * @param password Account password
     * @param firstName User's first name
     * @param lastName User's last name
     *
     * @return EphemeralCredentials if successful, null otherwise
     */
    suspend operator fun invoke(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
    ): EphemeralCredentials = accountRepository.createAccount(
        email = email,
        password = password,
        firstName = firstName,
        lastName = lastName
    )
}