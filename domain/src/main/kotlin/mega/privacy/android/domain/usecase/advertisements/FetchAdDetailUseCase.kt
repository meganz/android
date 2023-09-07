package mega.privacy.android.domain.usecase.advertisements

import mega.privacy.android.domain.entity.advertisements.AdDetail
import mega.privacy.android.domain.entity.advertisements.FetchAdDetailRequest
import mega.privacy.android.domain.repository.AdsRepository
import javax.inject.Inject

/**
 * Fetch the ad details for specific ad slot on the screen
 */
class FetchAdDetailUseCase @Inject constructor(
    private val adsRepository: AdsRepository,
) {
    /**
     * Invoke
     * return first AdDetail object from the list (should be the only one in the list)
     * @param fetchAdDetailRequest [FetchAdDetailRequest]
     * @return [AdDetail]
     */
    suspend operator fun invoke(
        fetchAdDetailRequest: FetchAdDetailRequest,
    ): AdDetail =
        adsRepository.fetchAdDetails(
            listOf(fetchAdDetailRequest.slotId),
            fetchAdDetailRequest.linkHandle
        ).first { adDetail ->
            adDetail.slotId == fetchAdDetailRequest.slotId
        }
}