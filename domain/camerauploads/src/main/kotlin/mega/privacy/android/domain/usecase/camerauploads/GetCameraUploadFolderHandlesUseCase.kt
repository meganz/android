package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Resolves the Camera Upload and Media Upload folder handles used to scope media by source.
 *
 * The locally-stored sync handles are used first, falling back to the handles stored server-side
 * when the local preferences are not populated (e.g. on a device where Camera Uploads has never been
 * configured). Handles that are not configured (the invalid handle) are dropped.
 *
 * @property cameraUploadsRepository [CameraUploadsRepository]
 */
class GetCameraUploadFolderHandlesUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
) {

    /**
     * Invoke.
     *
     * @return the Camera Upload + Media Upload folder handles as [NodeId]s, or empty when none are
     * configured.
     */
    suspend operator fun invoke(): List<NodeId> = with(cameraUploadsRepository) {
        val invalidHandle = getInvalidHandle()
        val localHandles = listOfNotNull(getPrimarySyncHandle(), getSecondarySyncHandle())
            .filter { it != invalidHandle }
        localHandles
            .ifEmpty {
                getCameraUploadsSyncHandles()
                    ?.toList()
                    .orEmpty()
                    .filter { it != invalidHandle }
            }
            .map { NodeId(it) }
    }
}
