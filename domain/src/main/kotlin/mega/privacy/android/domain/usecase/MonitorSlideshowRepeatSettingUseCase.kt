package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.repository.SlideshowRepository
import javax.inject.Inject

/**
 * Monitor slideshow repeat setting use case
 */
class MonitorSlideshowRepeatSettingUseCase @Inject constructor(
    private val slideshowRepository: SlideshowRepository,
) {
    operator fun invoke(): Flow<Boolean?> = slideshowRepository.monitorRepeatSetting()
}
