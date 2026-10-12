package mega.privacy.android.domain.usecase.advertisements

import mega.privacy.android.domain.repository.AdsRepository
import javax.inject.Inject

/**
 * Use case to reset the rewarded ad attempt count back to 0.
 */
class ResetRewardedAdAttemptCountUseCase @Inject constructor(
    private val adsRepository: AdsRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke() = adsRepository.resetRewardedAdAttemptCount()
}
