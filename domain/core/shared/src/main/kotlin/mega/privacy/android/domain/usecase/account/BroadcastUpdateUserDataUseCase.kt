package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * A use case to broadcast the update user data event
 */
class BroadcastUpdateUserDataUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {

    /**
     * Notify the repository to broadcast the update user data event
     */
    suspend operator fun invoke() = accountRepository.broadcastUpdateUserData()
}
