package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.slideshow.SlideshowOrder
import mega.privacy.android.domain.repository.SlideshowRepository
import javax.inject.Inject

/**
 * Monitor slideshow order setting use case
 */
class MonitorSlideshowOrderSettingUseCase @Inject constructor(
    private val slideshowRepository: SlideshowRepository,
) {
    operator fun invoke(): Flow<SlideshowOrder?> = slideshowRepository.monitorOrderSetting()
}
