package mega.privacy.android.domain.usecase.banner

import mega.privacy.android.domain.entity.banner.PromotionalBanner
import javax.inject.Inject

/**
 * Use case to get banners
 */
class GetPromoBannersUseCase @Inject constructor(
    private val getBannersUseCase: GetBannersUseCase,
) {
    /**
     * Get Promotional banners
     */
    suspend operator fun invoke(): List<PromotionalBanner> {
        return getBannersUseCase().map { banner ->
            PromotionalBanner(
                id = banner.id,
                title = banner.title,
                buttonText = banner.buttonText.orEmpty(),
                image = "${banner.imageLocation}/${banner.image}",
                backgroundImage = "${banner.imageLocation}/${banner.backgroundImage}",
                url = banner.url,
                imageLocation = banner.imageLocation
            )
        }
    }
}
