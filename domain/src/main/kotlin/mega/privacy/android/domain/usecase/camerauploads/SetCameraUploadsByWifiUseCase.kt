package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Use Case to set whether Camera Uploads should only run through Wi-Fi / Wi-Fi or Mobile Data
 *
 * @property cameraUploadRepository [CameraUploadRepository]
 */
class SetCameraUploadsByWifiUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) {

    /**
     * Invocation function
     *
     * @param wifiOnly If true, Camera Uploads will only run through Wi-Fi
     * If false, Camera Uploads can run through either Wi-Fi or Mobile Data
     */
    suspend operator fun invoke(wifiOnly: Boolean) =
        cameraUploadRepository.setCameraUploadsByWifi(wifiOnly)
}