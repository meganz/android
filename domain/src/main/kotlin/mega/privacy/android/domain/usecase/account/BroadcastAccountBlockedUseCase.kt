package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use case for broadcasting blocked account.
 */
class BroadcastAccountBlockedUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {

    /**
     * Invoke.
     *
     * @param type Blocked account type.
     * @param text Message.
     */
    suspend operator fun invoke(type: Long, text: String) =
        accountRepository.broadcastAccountBlocked(type, text)
}