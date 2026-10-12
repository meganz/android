package mega.privacy.android.domain.usecase.advertisements

import mega.privacy.android.domain.repository.AdsRepository
import javax.inject.Inject

/**
 * Use case to query ads
 */
class QueryAdsUseCase @Inject constructor(private val adsRepository: AdsRepository) {
    /**
     * Invoke
     *
     * @return [Boolean] true if ads should be shown, false otherwise
     */
    suspend operator fun invoke(linkHandle: Long): Boolean = adsRepository.queryAds(linkHandle)
}