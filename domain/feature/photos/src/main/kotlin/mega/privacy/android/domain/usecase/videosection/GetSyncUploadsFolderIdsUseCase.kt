package mega.privacy.android.domain.usecase.videosection

import mega.privacy.android.domain.repository.PhotosRepository
import javax.inject.Inject

/**
 * Use case to get the camera uploads and media uploads folder ids
 */
class GetSyncUploadsFolderIdsUseCase @Inject constructor(
    private val photosRepository: PhotosRepository,
) {

    /**
     * Get the camera uploads and media uploads folder ids
     */
    suspend operator fun invoke() = listOfNotNull(
        photosRepository.getCameraUploadFolderId(),
        photosRepository.getMediaUploadFolderId()
    )
}