package mega.privacy.android.domain.usecase.slideshow

import mega.privacy.android.domain.repository.SlideshowRepository
import javax.inject.Inject

/**
 * Use case to monitor if the secure slideshow tutorial has been shown
 */
class MonitorSecureSlideshowTutorialShownUseCase @Inject constructor(
    private val slideshowRepository: SlideshowRepository,
) {
    operator fun invoke() = slideshowRepository.monitorSecureSlideshowTutorialShown()
}