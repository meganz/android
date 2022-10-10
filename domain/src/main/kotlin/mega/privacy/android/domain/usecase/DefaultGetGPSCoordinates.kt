package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Default implementation of [GetGPSCoordinates]
 *
 * @property cameraUploadRepository CameraUploadRepository
 */
class DefaultGetGPSCoordinates @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) : GetGPSCoordinates {

    override suspend fun invoke(filePath: String, isVideo: Boolean): Pair<Float, Float> {
        return if (isVideo) {
            cameraUploadRepository.getVideoGPSCoordinates(filePath)
        } else {
            cameraUploadRepository.getPhotoGPSCoordinates(filePath)
        }
    }
}
