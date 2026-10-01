package mega.privacy.android.domain.usecase.business

import mega.privacy.android.domain.repository.BusinessRepository
import javax.inject.Inject

/**
 * Monitor business account expired events
 */
class MonitorBusinessAccountExpiredUseCase @Inject constructor(
    private val businessRepository: BusinessRepository,
) {
    /**
     * Monitor business account expired events
     *
     * @return a flow that emits each time a new business account expired error is received
     */
    operator fun invoke() = businessRepository.monitorBusinessAccountExpired()
}