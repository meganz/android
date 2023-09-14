package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.MediaStoreFileType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsMedia
import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Retrieve a list of [CameraUploadsMedia] from the media store
 */
class RetrieveMediaFromMediaStoreUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) {

    /**
     * Retrieve a list of [CameraUploadsMedia] from the media store
     *
     * @param parentPath used for filtering the media contained in the parent path
     * @param types types of files that we want to retrieve. This types will be converted to proper Uri
     *
     * @return a list of [CameraUploadsMedia]
     */
    suspend operator fun invoke(
        parentPath: String,
        types: List<MediaStoreFileType>,
    ): List<CameraUploadsMedia> {
        val selectionQuery = cameraUploadRepository.getMediaSelectionQuery(parentPath)
        return types.flatMap {
            cameraUploadRepository.getMediaList(
                mediaStoreFileType = it,
                selectionQuery = selectionQuery,
            )
        }
    }
}

