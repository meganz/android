package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.camerauploads.GetUploadFolderHandleUseCase
import javax.inject.Inject

/**
 * Check if local folder attribute changed and reset timelines
 *
 */
class DefaultResetCameraUploadTimelines @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val getUploadFolderHandleUseCase: GetUploadFolderHandleUseCase,
    private val resetPrimaryTimeline: ResetPrimaryTimeline,
    private val resetSecondaryTimeline: ResetSecondaryTimeline,
) : ResetCameraUploadTimelines {

    override suspend fun invoke(
        handleInAttribute: Long,
        cameraUploadFolderType: CameraUploadFolderType,
    ): Boolean {
        if (handleInAttribute == cameraUploadRepository.getInvalidHandle()) {
            return false
        }
        if (handleInAttribute != getUploadFolderHandleUseCase(cameraUploadFolderType)) {
            return when (cameraUploadFolderType) {
                CameraUploadFolderType.Primary -> {
                    resetPrimaryTimeline()
                    true
                }

                CameraUploadFolderType.Secondary -> {
                    resetSecondaryTimeline()
                    true
                }
            }
        }
        return false
    }
}
