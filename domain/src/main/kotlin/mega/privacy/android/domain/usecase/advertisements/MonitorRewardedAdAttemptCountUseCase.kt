package mega.privacy.android.domain.usecase.advertisements

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.repository.AdsRepository
import javax.inject.Inject

/**
 * Use case to monitor the rewarded ad attempt count.
 */
class MonitorRewardedAdAttemptCountUseCase @Inject constructor(
    private val adsRepository: AdsRepository,
) {
    /**
     * Invoke
     * @return [Flow] of the current rewarded ad attempt count.
     */
    operator fun invoke(): Flow<Int> = adsRepository.monitorRewardedAdAttemptCount()
}
