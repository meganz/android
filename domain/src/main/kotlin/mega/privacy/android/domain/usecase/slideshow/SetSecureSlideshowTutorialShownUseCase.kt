package mega.privacy.android.domain.usecase.slideshow

import mega.privacy.android.domain.repository.SlideshowRepository
import javax.inject.Inject

/**
 * Set secure slideshow tutorial shown use case
 */
class SetSecureSlideshowTutorialShownUseCase @Inject constructor(
    private val slideshowRepository: SlideshowRepository,
) {
    suspend operator fun invoke() = slideshowRepository.setSecureSlideshowTutorialShown()
}