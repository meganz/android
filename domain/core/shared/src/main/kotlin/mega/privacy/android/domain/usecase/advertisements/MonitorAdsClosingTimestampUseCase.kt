package mega.privacy.android.domain.usecase.advertisements

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.repository.AdsRepository
import javax.inject.Inject

/**
 * Use case to monitor ads closing timestamp
 */
class MonitorAdsClosingTimestampUseCase @Inject constructor(
    private val adsRepository: AdsRepository,
) {
    /**
     * Invoke
     * @return [Flow] of [Long?]
     */
    operator fun invoke(): Flow<Long?> = adsRepository.monitorAdsClosingTimestamp()
}