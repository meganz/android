package mega.privacy.android.domain.usecase.banner

import mega.privacy.android.domain.entity.banner.Banner
import mega.privacy.android.domain.repository.BannerRepository
import mega.privacy.android.domain.usecase.GetDeviceCurrentTimeUseCase
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours

/**
 * Use case to get banners
 */
class GetBannersUseCase @Inject constructor(
    private val bannerRepository: BannerRepository,
    private val getDeviceCurrentTimeUseCase: GetDeviceCurrentTimeUseCase,
) {
    /**
     * Get banners
     */
    suspend operator fun invoke(): List<Banner> {
        val currentTime = getDeviceCurrentTimeUseCase()
        val forceRefresh = (currentTime - bannerRepository.getLastFetchedBannerTime()) > FIVE_HOURS
        return bannerRepository.getBanners(forceRefresh)
    }

    companion object {
        private val FIVE_HOURS = 5.hours.inWholeMilliseconds
    }
}