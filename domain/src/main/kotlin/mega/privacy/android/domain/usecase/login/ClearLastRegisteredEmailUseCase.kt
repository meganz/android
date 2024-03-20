package mega.privacy.android.domain.usecase.login

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use case for clearing the email that was lastly attempted to be registered
 *
 * @property accountRepository [AccountRepository]
 */
class ClearLastRegisteredEmailUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    /**
     * Invoke
     * return [Unit]
     */
    suspend operator fun invoke() = accountRepository.clearLastRegisteredEmail()
}