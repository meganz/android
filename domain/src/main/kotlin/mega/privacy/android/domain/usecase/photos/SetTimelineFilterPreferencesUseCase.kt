package mega.privacy.android.domain.usecase.photos

import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * Use case to set the timeline filter preferences
 */
class SetTimelineFilterPreferencesUseCase @Inject constructor(
    private val photosRepository: PhotosRepository,
) {
    suspend operator fun invoke(preferences: Map<String, String>) =
        photosRepository.setTimelineFilterPreferences(preferences)
}