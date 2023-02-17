package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Default implementation of [SetupPrimaryFolder]
 */
class DefaultSetupPrimaryFolder @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val resetPrimaryTimeline: ResetPrimaryTimeline,
    private val setPrimarySyncHandle: SetPrimarySyncHandle,
) : SetupPrimaryFolder {
    override suspend fun invoke(primaryHandle: Long) {
        cameraUploadRepository.setupPrimaryFolder(primaryHandle)
            .takeIf { it != cameraUploadRepository.getInvalidHandle() }
            ?.let {
                resetPrimaryTimeline()
                setPrimarySyncHandle(it)
            }
    }
}
