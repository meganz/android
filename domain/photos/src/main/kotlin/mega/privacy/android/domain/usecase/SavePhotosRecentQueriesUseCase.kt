package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

class SavePhotosRecentQueriesUseCase @Inject constructor(
    private val photosRepository: PhotosRepository,
) {
    suspend operator fun invoke(queries: List<String>) {
        photosRepository.saveRecentQueries(queries)
    }
}
