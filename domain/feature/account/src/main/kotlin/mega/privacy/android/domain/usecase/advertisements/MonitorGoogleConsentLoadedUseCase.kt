package mega.privacy.android.domain.usecase.advertisements

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import mega.privacy.android.domain.repository.AdsRepository
import javax.inject.Inject

/**
 * Use case to monitor Google consent loaded state
 */
class MonitorGoogleConsentLoadedUseCase @Inject constructor(
    private val adsRepository: AdsRepository,
) {
    /**
     * Invoke
     * @return [Flow] of [Boolean]
     */
    operator fun invoke(): Flow<Boolean> =
        adsRepository.monitorGoogleConsentLoaded().distinctUntilChanged()
}
