package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.SlideshowRepository
import javax.inject.Inject

/**
 * Save slideshow repeat setting use case
 */
class SaveSlideshowRepeatSettingUseCase @Inject constructor(
    private val slideshowRepository: SlideshowRepository,
) {
    suspend operator fun invoke(isRepeat: Boolean) = slideshowRepository.saveRepeatSetting(isRepeat)
}
