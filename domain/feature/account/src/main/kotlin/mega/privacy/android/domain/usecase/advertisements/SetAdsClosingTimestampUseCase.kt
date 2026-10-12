package mega.privacy.android.domain.usecase.advertisements

import mega.privacy.android.domain.repository.AdsRepository
import javax.inject.Inject

/**
 * Use case to set ads closing timestamp
 */
class SetAdsClosingTimestampUseCase @Inject constructor(
    private val adsRepository: AdsRepository,
) {
    /**
     * Invoke
     * @param timestamp [Long]
     */
    suspend operator fun invoke(timestamp: Long) = adsRepository.setAdsClosingTimestamp(timestamp)
}