package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.repository.CameraUploadsRepository
import javax.inject.Inject

/**
 * Use Case to set whether Camera Uploads should only run through Wi-Fi / Wi-Fi or Mobile Data
 *
 * @property cameraUploadsRepository [CameraUploadsRepository]
 */
class SetCameraUploadsByWifiUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
) {

    /**
     * Invocation function
     *
     * @param wifiOnly If true, Camera Uploads will only run through Wi-Fi
     * If false, Camera Uploads can run through either Wi-Fi or Mobile Data
     */
    suspend operator fun invoke(wifiOnly: Boolean) =
        cameraUploadsRepository.setCameraUploadsByWifi(wifiOnly)
}