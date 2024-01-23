package mega.privacy.android.domain.usecase.advertisements

import mega.privacy.android.domain.entity.advertisements.AdDetails
import mega.privacy.android.domain.entity.advertisements.FetchAdDetailRequest
import mega.privacy.android.domain.entity.settings.cookie.CookieType
import mega.privacy.android.domain.repository.AdsRepository
import mega.privacy.android.domain.usecase.setting.GetCookieSettingsUseCase
import javax.inject.Inject

/**
 * Fetch the ad details for specific ad slot on the screen
 */
class FetchAdDetailUseCase @Inject constructor(
    private val adsRepository: AdsRepository,
    private val getCookieSettingsUseCase: GetCookieSettingsUseCase,
) {
    /**
     * Invoke
     * return first AdDetail object from the list (should be the only one in the list)
     * @param fetchAdDetailRequest [FetchAdDetailRequest]
     * @return [AdDetails]
     */
    suspend operator fun invoke(
        fetchAdDetailRequest: FetchAdDetailRequest,
    ): AdDetails? {

        val adDetails = adsRepository.fetchAdDetails(
            listOf(fetchAdDetailRequest.slotId),
            fetchAdDetailRequest.linkHandle
        ).firstOrNull { adDetails ->
            adDetails.slotId == fetchAdDetailRequest.slotId
        }

        if (adDetails == null) {
            return null
        } else {
            val isAdsCookieEnabled =
                getCookieSettingsUseCase().containsAll(
                    listOf(
                        CookieType.ADS_CHECK,
                        CookieType.ADVERTISEMENT
                    )
                )
            return if (isAdsCookieEnabled) {
                adDetails.copy(slotId = adDetails.slotId, url = "${adDetails.url}&ads=1")
            } else {
                adDetails
            }
        }
    }
}
