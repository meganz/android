package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.slideshow.SlideshowSpeed
import mega.privacy.android.domain.repository.SlideshowRepository
import javax.inject.Inject

/**
 * Save slideshow speed setting use case
 */
class SaveSlideshowSpeedSettingUseCase @Inject constructor(
    private val slideshowRepository: SlideshowRepository,
) {
    suspend operator fun invoke(speed: SlideshowSpeed) = slideshowRepository.saveSpeedSetting(speed)
}
