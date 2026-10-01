package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.slideshow.SlideshowOrder
import mega.privacy.android.domain.repository.SlideshowRepository
import javax.inject.Inject

/**
 * Save slideshow order setting use case
 */
class SaveSlideshowOrderSettingUseCase @Inject constructor(
    private val slideshowRepository: SlideshowRepository,
) {
    suspend operator fun invoke(order: SlideshowOrder) = slideshowRepository.saveOrderSetting(order)
}
