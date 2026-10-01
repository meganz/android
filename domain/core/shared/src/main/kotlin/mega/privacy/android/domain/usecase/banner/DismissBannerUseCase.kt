package mega.privacy.android.domain.usecase.banner

import mega.privacy.android.domain.repository.BannerRepository
import javax.inject.Inject

/**
 * Use case to dismiss a banner
 */
class DismissBannerUseCase @Inject constructor(
    private val bannerRepository: BannerRepository,
) {
    /**
     * Dismiss a banner
     *
     * @param bannerId The banner id
     */
    suspend operator fun invoke(bannerId: Int) = bannerRepository.dismissBanner(bannerId)
}