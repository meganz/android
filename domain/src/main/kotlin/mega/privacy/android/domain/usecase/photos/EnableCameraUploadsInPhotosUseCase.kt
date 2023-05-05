package mega.privacy.android.domain.usecase.photos

import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.repository.SettingsRepository
import mega.privacy.android.domain.usecase.camerauploads.ListenToNewMediaUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetCameraUploadsByWifiUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetChargingRequiredForVideoCompressionUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetDefaultPrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetUploadVideoQualityUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetVideoCompressionSizeLimitUseCase
import javax.inject.Inject

/**
 * Use Case to enable Camera Uploads in the Photos feature
 *
 * @property listenToNewMediaUseCase [ListenToNewMediaUseCase]
 * @property settingsRepository [SettingsRepository]
 * @property setCameraUploadsByWifiUseCase [SetCameraUploadsByWifiUseCase]
 * @property setChargingRequiredForVideoCompressionUseCase [SetChargingRequiredForVideoCompressionUseCase]
 * @property setDefaultPrimaryFolderPathUseCase [SetDefaultPrimaryFolderPathUseCase]
 * @property setUploadVideoQualityUseCase [SetUploadVideoQualityUseCase]
 * @property setVideoCompressionSizeLimitUseCase [SetVideoCompressionSizeLimitUseCase]
 */
class EnableCameraUploadsInPhotosUseCase @Inject constructor(
    private val listenToNewMediaUseCase: ListenToNewMediaUseCase,
    private val settingsRepository: SettingsRepository,
    private val setCameraUploadsByWifiUseCase: SetCameraUploadsByWifiUseCase,
    private val setChargingRequiredForVideoCompressionUseCase: SetChargingRequiredForVideoCompressionUseCase,
    private val setDefaultPrimaryFolderPathUseCase: SetDefaultPrimaryFolderPathUseCase,
    private val setUploadVideoQualityUseCase: SetUploadVideoQualityUseCase,
    private val setVideoCompressionSizeLimitUseCase: SetVideoCompressionSizeLimitUseCase,
) {

    /**
     * Invocation function
     *
     * @param shouldSyncVideos Whether to include videos for uploading or not
     * @param shouldUseWiFiOnly true if Camera Uploads will only run through Wi-Fi, and false if
     * Camera Uploads can run through either Wi-Fi or Mobile Data
     * @param videoCompressionSizeLimit The maximum video file size that can be compressed
     * @param videoUploadQuality The Video upload quality
     */
    suspend operator fun invoke(
        shouldSyncVideos: Boolean,
        shouldUseWiFiOnly: Boolean,
        videoCompressionSizeLimit: Int,
        videoUploadQuality: VideoQuality,
    ) {
        setDefaultPrimaryFolderPathUseCase()
        setCameraUploadsByWifiUseCase(shouldUseWiFiOnly)
        settingsRepository.setCameraUploadFileType(shouldSyncVideos)
        setUploadVideoQualityUseCase(videoUploadQuality)
        setChargingRequiredForVideoCompressionUseCase(true)
        setVideoCompressionSizeLimitUseCase(videoCompressionSizeLimit)
        settingsRepository.setEnableCameraUpload(true)
        listenToNewMediaUseCase()
    }
}