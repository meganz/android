package mega.privacy.android.domain.usecase.account

import mega.privacy.android.domain.entity.MyAccountUpdate
import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Broadcast My Account Update Use Case
 */
class BroadcastMyAccountUpdateUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke(data: MyAccountUpdate) =
        accountRepository.broadcastMyAccountUpdate(data)
}