package mega.privacy.android.domain.usecase.setting

import mega.privacy.android.domain.entity.featureflag.MiscLoadedState
import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use Case to broadcast misc state
 */
class BroadcastMiscStateUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    /**
     * Invoke the use case
     *
     * @param state The misc state to broadcast
     */
    suspend operator fun invoke(state: MiscLoadedState) {
        accountRepository.broadcastMiscState(state)
    }
}

