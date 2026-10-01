package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use case for checking if the current session is Ephemeral Plus Plus
 */
class IsEphemeralPlusPlusUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {

    /**
     * Invoke
     *
     * @return  true if it's Ephemeral Plus Plus account, false otherwise.
     */
    suspend operator fun invoke(): Boolean =
        accountRepository.isEphemeralPlusPlus()
}
