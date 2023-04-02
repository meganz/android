package mega.privacy.android.domain.usecase.photos

import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * Filter Favourite photos
 */
class FilterFavouriteUseCase @Inject constructor(
    private val photosRepository: PhotosRepository,
) {
    /**
     * Filter Favourite photos
     *
     * @return is favourite
     */
    operator fun invoke(): suspend (photo: Photo) -> Boolean =
        {
            it.isFavourite
                    && (it is Photo.Video && inSyncFolder(it.parentId)
                    || it is Photo.Image)
        }

    private suspend fun inSyncFolder(parentId: Long): Boolean =
        parentId == photosRepository.getCameraUploadFolderId()
                || parentId == photosRepository.getMediaUploadFolderId()

}