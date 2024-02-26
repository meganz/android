package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * A use case to get the user's data
 */
class GetUserDataUseCase @Inject constructor(
    private val broadcastUpdateUserDataUseCase: BroadcastUpdateUserDataUseCase,
    private val accountRepository: AccountRepository,
) {

    /**
     * Get the user's data from the account repository
     */
    suspend operator fun invoke() {
        accountRepository.getUserData()
        broadcastUpdateUserDataUseCase()
    }
}
