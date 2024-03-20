package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 *  Use case for retrieving the email that was lastly attempted to be registered
 *
 *  @property accountRepository [AccountRepository]
 */
class GetLastRegisteredEmailUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {

    /**
     * Invoke.
     *
     * @return [String?]
     */
    suspend operator fun invoke() = accountRepository.getLastRegisteredEmail()
}