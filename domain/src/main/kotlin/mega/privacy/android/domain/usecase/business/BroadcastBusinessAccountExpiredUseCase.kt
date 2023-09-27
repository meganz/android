package mega.privacy.android.domain.usecase.business

import mega.privacy.android.domain.repository.BusinessRepository
import javax.inject.Inject

/**
 * Broadcast business account expired use case
 */
class BroadcastBusinessAccountExpiredUseCase @Inject constructor(
    private val businessRepository: BusinessRepository
) {

    /**
     * Broadcast business account expired
     */
    suspend operator fun invoke() {
        businessRepository.broadcastBusinessAccountExpired()
    }
}