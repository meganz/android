package mega.privacy.android.domain.usecase.imagepreview

import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * Clear image result from cache
 */
class ClearImageResultUseCase @Inject constructor(
    private val photosRepository: PhotosRepository,
) {
    operator fun invoke(uncompletedOnly: Boolean) =
        photosRepository.clearImageResult(uncompletedOnly)
}
