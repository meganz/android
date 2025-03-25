package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

class RetrievePhotosRecentQueriesUseCase @Inject constructor(
    private val photosRepository: PhotosRepository,
) {
    suspend operator fun invoke(): List<String> = photosRepository.retrieveRecentQueries()
}
