package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use case for saving the email that was lastly attempted to be registered
 *
 * @property accountRepository [AccountRepository] Repository to provide the related operations
 */
class SaveLastRegisteredEmailUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    /**
     * Invoke
     *
     * @param email [String]
     */
    suspend operator fun invoke(email: String) =
        accountRepository.saveLastRegisteredEmail(email)
}