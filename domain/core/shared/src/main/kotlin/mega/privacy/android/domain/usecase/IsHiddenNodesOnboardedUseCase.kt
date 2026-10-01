package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * The use case to check is hidden nodes onboarded
 */
class IsHiddenNodesOnboardedUseCase @Inject constructor(
    private val photosRepository: PhotosRepository,
) {
    /**
     * Invoke
     */
    suspend operator fun invoke(): Boolean {
        return photosRepository.isHiddenNodesOnboarded()
    }
}
