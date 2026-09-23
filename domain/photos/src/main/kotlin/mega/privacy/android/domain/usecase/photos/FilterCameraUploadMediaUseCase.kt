package mega.privacy.android.domain.usecase.photos

import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.repository.CameraUploadsRepository
import mega.privacy.android.domain.repository.PhotosRepository
import mega.privacy.android.domain.usecase.camerauploads.GetCameraUploadsSyncHandlesUseCase
import javax.inject.Inject

class FilterCameraUploadMediaUseCase @Inject constructor(
    private val photosRepository: PhotosRepository,
    private val getCameraUploadsSyncHandlesUseCase: GetCameraUploadsSyncHandlesUseCase,
    private val cameraUploadsRepository: CameraUploadsRepository,
) {

    suspend operator fun invoke(source: List<TypedFileNode>) =
        createTempSyncFolderIds().let { sync ->
            source.filter { it.parentId.longValue in sync }
        }

    private suspend fun createTempSyncFolderIds(): List<Long> {
        val localFolderIds = listOfNotNull(
            photosRepository.getCameraUploadFolderId(),
            photosRepository.getMediaUploadFolderId()
        )
        // On a clean install the local Camera Uploads preferences are not yet populated, so fall
        // back to the folder handles resolved from the server (the camera uploads user attribute).
        return localFolderIds.ifEmpty {
            val invalidHandle = cameraUploadsRepository.getInvalidHandle()
            getCameraUploadsSyncHandlesUseCase()
                ?.toList()
                .orEmpty()
                .filter { it != invalidHandle }
        }
    }
}
