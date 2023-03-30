package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.repository.SettingsRepository
import mega.privacy.android.domain.usecase.camerauploads.SetCameraUploadsByWifiUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetChargingRequiredForVideoCompression
import mega.privacy.android.domain.usecase.camerauploads.SetUploadVideoQualityUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetVideoCompressionSizeLimit
import javax.inject.Inject

/**
 * Default implementation of [EnablePhotosCameraUpload]
 *
 * @property settingsRepository [SettingsRepository]
 * @property setCameraUploadsByWifiUseCase [SetCameraUploadsByWifiUseCase]
 * @property setChargingRequiredForVideoCompression [SetChargingRequiredForVideoCompression]
 * @property setUploadVideoQualityUseCase [SetUploadVideoQualityUseCase]
 * @property setVideoCompressionSizeLimit [SetVideoCompressionSizeLimit]
 * @property cameraUploadRepository [CameraUploadRepository]
 */
class DefaultEnablePhotosCameraUpload @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val setCameraUploadsByWifiUseCase: SetCameraUploadsByWifiUseCase,
    private val setChargingRequiredForVideoCompression: SetChargingRequiredForVideoCompression,
    private val setUploadVideoQualityUseCase: SetUploadVideoQualityUseCase,
    private val setVideoCompressionSizeLimit: SetVideoCompressionSizeLimit,
    private val cameraUploadRepository: CameraUploadRepository,
) : EnablePhotosCameraUpload {

    override suspend fun invoke(
        path: String?,
        syncVideo: Boolean,
        enableCellularSync: Boolean,
        videoQuality: VideoQuality,
        conversionChargingOnSize: Int,
    ) {
        with(settingsRepository) {
            setCameraUploadLocalPath(path)
            setCameraUploadsByWifiUseCase(!enableCellularSync)
            setCameraUploadFileType(syncVideo)
            setCameraFolderExternalSDCard(false)
            setUploadVideoQualityUseCase(videoQuality)
            setChargingRequiredForVideoCompression(true)
            setVideoCompressionSizeLimit(conversionChargingOnSize)
            // After target and local folder setup, then enable CU.
            setEnableCameraUpload(true)
        }
        cameraUploadRepository.listenToNewMedia()
    }
}
