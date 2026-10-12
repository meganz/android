package mega.privacy.android.domain.usecase.advertisements

import mega.privacy.android.domain.repository.AdsRepository
import javax.inject.Inject

/**
 * Use case to atomically increment the rewarded ad attempt count by 1.
 */
class IncrementRewardedAdAttemptCountUseCase @Inject constructor(
    private val adsRepository: AdsRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke() = adsRepository.incrementRewardedAdAttemptCount()
}
