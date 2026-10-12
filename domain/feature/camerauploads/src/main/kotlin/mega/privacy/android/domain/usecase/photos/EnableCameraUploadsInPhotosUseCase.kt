package mega.privacy.android.domain.usecase.photos

import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption
import mega.privacy.android.domain.repository.CameraUploadsRepository
import mega.privacy.android.domain.usecase.camerauploads.ListenToNewMediaUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetCameraUploadsByWifiUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetChargingRequiredForVideoCompressionUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetDefaultPrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetUploadVideoQualityUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetVideoCompressionSizeLimitUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupCameraUploadsSettingUseCase
import javax.inject.Inject

/**
 * Use Case to enable Camera Uploads in the Photos feature
 *
 * @property listenToNewMediaUseCase [ListenToNewMediaUseCase]
 * @property cameraUploadsRepository [CameraUploadsRepository]
 * @property setCameraUploadsByWifiUseCase [SetCameraUploadsByWifiUseCase]
 * @property setChargingRequiredForVideoCompressionUseCase [SetChargingRequiredForVideoCompressionUseCase]
 * @property setDefaultPrimaryFolderPathUseCase [SetDefaultPrimaryFolderPathUseCase]
 * @property setUploadVideoQualityUseCase [SetUploadVideoQualityUseCase]
 * @property setVideoCompressionSizeLimitUseCase [SetVideoCompressionSizeLimitUseCase]
 */
class EnableCameraUploadsInPhotosUseCase @Inject constructor(
    private val listenToNewMediaUseCase: ListenToNewMediaUseCase,
    private val cameraUploadsRepository: CameraUploadsRepository,
    private val setCameraUploadsByWifiUseCase: SetCameraUploadsByWifiUseCase,
    private val setChargingRequiredForVideoCompressionUseCase: SetChargingRequiredForVideoCompressionUseCase,
    private val setDefaultPrimaryFolderPathUseCase: SetDefaultPrimaryFolderPathUseCase,
    private val setUploadVideoQualityUseCase: SetUploadVideoQualityUseCase,
    private val setVideoCompressionSizeLimitUseCase: SetVideoCompressionSizeLimitUseCase,
    private val setupCameraUploadsSettingUseCase: SetupCameraUploadsSettingUseCase,
) {

    /**
     * Invocation function
     *
     * @param videoCompressionSizeLimit The maximum video file size that can be compressed
     * @param videoUploadQuality The Video upload quality
     * @param includeVideos true if videos should be included in camera uploads, false otherwise
     * @param wifiOnly true if uploads should be done on wifi only, false otherwise
     */
    suspend operator fun invoke(
        videoCompressionSizeLimit: Int,
        videoUploadQuality: VideoQuality = VideoQuality.ORIGINAL,
        includeVideos: Boolean = true,
        wifiOnly: Boolean = true,
    ) {
        setDefaultPrimaryFolderPathUseCase()
        setCameraUploadsByWifiUseCase(wifiOnly)
        cameraUploadsRepository.setUploadOption(if (includeVideos) UploadOption.PHOTOS_AND_VIDEOS else UploadOption.PHOTOS)
        setUploadVideoQualityUseCase(videoUploadQuality)
        setChargingRequiredForVideoCompressionUseCase(true)
        setVideoCompressionSizeLimitUseCase(videoCompressionSizeLimit)
        setupCameraUploadsSettingUseCase(isEnabled = true)
        listenToNewMediaUseCase(forceEnqueue = false)
    }
}
