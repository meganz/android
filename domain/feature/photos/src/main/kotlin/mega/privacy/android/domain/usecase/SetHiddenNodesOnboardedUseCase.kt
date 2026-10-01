package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * The use case to mark hidden nodes onboarding shown
 */
class SetHiddenNodesOnboardedUseCase @Inject constructor(
    private val photosRepository: PhotosRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke() = photosRepository.setHiddenNodesOnboarded()
}
