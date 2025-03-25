package mega.privacy.android.domain.usecase.setting

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use Case to broadcast that misc data has been loaded
 */
class BroadcastMiscLoadedUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    /**
     * Invoke the use case
     */
    suspend operator fun invoke() = accountRepository.broadcastMiscLoaded()
}