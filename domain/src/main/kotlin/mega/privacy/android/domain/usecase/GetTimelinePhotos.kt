package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.photos.Photo

/**
 * The use case interface to get Timeline photos
 */
interface GetTimelinePhotos {

    /**
     * Get timeline photos
     *
     * @return Flow<List<Photo>>
     */
    operator fun invoke(): Flow<List<Photo>>
}