package mega.privacy.android.domain.usecase.slideshow

import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * Get photo by public link
 */
class GetPhotoByPublicLinkUseCase @Inject constructor(
    private val photosRepository: PhotosRepository,
) {
    /**
     * Get Photo by public link
     * @return photo
     */
    suspend operator fun invoke(link: String): Photo? =
        photosRepository.getPhotoByPublicLink(link = link)
}