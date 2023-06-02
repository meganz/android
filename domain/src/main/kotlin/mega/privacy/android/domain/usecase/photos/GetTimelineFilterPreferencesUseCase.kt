package mega.privacy.android.domain.usecase.photos

import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * Use case to get the timeline filter preferences
 */
class GetTimelineFilterPreferencesUseCase @Inject constructor(
    private val photosRepository: PhotosRepository,
) {
    suspend operator fun invoke() = photosRepository.getTimelineFilterPreferences()
}