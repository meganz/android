package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Check if local folder attribute changed and reset timelines
 *
 */
class DefaultResetCameraUploadTimelines @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val getUploadFolderHandle: GetUploadFolderHandle,
    private val resetPrimaryTimeline: ResetPrimaryTimeline,
    private val resetSecondaryTimeline: ResetSecondaryTimeline,
) : ResetCameraUploadTimelines {

    override suspend fun invoke(handleInAttribute: Long, isSecondary: Boolean): Boolean {
        if (handleInAttribute == cameraUploadRepository.getInvalidHandle()) {
            return false
        }

        if (!isSecondary && handleInAttribute != getUploadFolderHandle(true)) {
            cameraUploadRepository.setPrimaryFolderHandle(handleInAttribute)
            resetPrimaryTimeline()
            return true
        } else if (isSecondary && handleInAttribute != getUploadFolderHandle(false)) {
            cameraUploadRepository.setSecondaryFolderHandle(handleInAttribute)
            resetSecondaryTimeline()
            return true
        }
        return false
    }
}
