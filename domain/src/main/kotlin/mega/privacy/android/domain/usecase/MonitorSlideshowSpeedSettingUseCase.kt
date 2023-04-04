package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.slideshow.SlideshowSpeed
import mega.privacy.android.domain.repository.SlideshowRepository
import javax.inject.Inject

/**
 * Monitor slideshow speed setting use case
 */
class MonitorSlideshowSpeedSettingUseCase @Inject constructor(
    private val slideshowRepository: SlideshowRepository,
) {
    operator fun invoke(): Flow<SlideshowSpeed?> = slideshowRepository.monitorSpeedSetting()
}
