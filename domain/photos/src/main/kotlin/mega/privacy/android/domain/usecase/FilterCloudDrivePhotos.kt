package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.photos.Photo

/**
 * The use case interface to filter photos from all mega photos
 */
interface FilterCloudDrivePhotos {
    /**
     * Filter cloud drive photos
     *
     * @return List<Photo>
     */
    suspend operator fun invoke(source: List<Photo>): List<Photo>
}