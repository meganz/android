package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Default Implementation of [DisableCameraUploadSettings]
 *
 */
class DefaultDisableCameraUploadSettings @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) : DisableCameraUploadSettings {

    override suspend fun invoke() {
        cameraUploadRepository.setCameraUploadsEnabled(false)
        cameraUploadRepository.setSecondaryEnabled(false)
    }
}
