package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.ResetPrimaryTimeline
import mega.privacy.android.domain.usecase.SetPrimarySyncHandle
import javax.inject.Inject

/**
 * Setup Primary Folder for Camera Upload
 *
 */
class SetupPrimaryFolderUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val resetPrimaryTimeline: ResetPrimaryTimeline,
    private val setPrimarySyncHandle: SetPrimarySyncHandle,
) {

    /**
     * Invoke
     *
     * @param primaryHandle
     */
    suspend operator fun invoke(primaryHandle: Long) {
        cameraUploadRepository.setupPrimaryFolder(primaryHandle)
            .takeIf { it != cameraUploadRepository.getInvalidHandle() }
            ?.let {
                resetPrimaryTimeline()
                setPrimarySyncHandle(it)
            }
    }
}
