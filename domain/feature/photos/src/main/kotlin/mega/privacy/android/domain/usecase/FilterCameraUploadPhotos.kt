package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.photos.Photo

/**
 * The use case interface to filter from all mega photos
 */
interface FilterCameraUploadPhotos {

    /**
     * Filter Camera Upload photos
     *
     * @return List<Photo>
     */
    suspend operator fun invoke(source: List<Photo>): List<Photo>
}