package mega.privacy.android.domain.usecase.photos

import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.repository.SettingsRepository
import mega.privacy.android.domain.usecase.camerauploads.ListenToNewMediaUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetCameraUploadsByWifiUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetChargingRequiredForVideoCompressionUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetPrimaryFolderInSDCardUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetPrimaryFolderLocalPathUseCase
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
 * @property setPrimaryFolderInSDCardUseCase [SetPrimaryFolderInSDCardUseCase]
 * @property setPrimaryFolderLocalPathUseCase [SetPrimaryFolderLocalPathUseCase]
 * @property setUploadVideoQualityUseCase [SetUploadVideoQualityUseCase]
 * @property setVideoCompressionSizeLimitUseCase [SetVideoCompressionSizeLimitUseCase]
 */
class EnableCameraUploadsInPhotosUseCase @Inject constructor(
    private val listenToNewMediaUseCase: ListenToNewMediaUseCase,
    private val settingsRepository: SettingsRepository,
    private val setCameraUploadsByWifiUseCase: SetCameraUploadsByWifiUseCase,
    private val setChargingRequiredForVideoCompressionUseCase: SetChargingRequiredForVideoCompressionUseCase,
    private val setPrimaryFolderInSDCardUseCase: SetPrimaryFolderInSDCardUseCase,
    private val setPrimaryFolderLocalPathUseCase: SetPrimaryFolderLocalPathUseCase,
    private val setUploadVideoQualityUseCase: SetUploadVideoQualityUseCase,
    private val setVideoCompressionSizeLimitUseCase: SetVideoCompressionSizeLimitUseCase,
) {

    /**
     * Invocation function
     *
     * @param primaryFolderLocalPath The Primary Folder local path
     * @param shouldSyncVideos Whether to include videos for uploading or not
     * @param shouldUseWiFiOnly true if Camera Uploads will only run through Wi-Fi, and false if
     * Camera Uploads can run through either Wi-Fi or Mobile Data
     * @param videoCompressionSizeLimit The maximum video file size that can be compressed
     * @param videoUploadQuality The Video upload quality
     */
    suspend operator fun invoke(
        primaryFolderLocalPath: String,
        shouldSyncVideos: Boolean,
        shouldUseWiFiOnly: Boolean,
        videoCompressionSizeLimit: Int,
        videoUploadQuality: VideoQuality,
    ) {
        setPrimaryFolderLocalPathUseCase(primaryFolderLocalPath)
        setCameraUploadsByWifiUseCase(shouldUseWiFiOnly)
        settingsRepository.setCameraUploadFileType(shouldSyncVideos)
        setPrimaryFolderInSDCardUseCase(false)
        setUploadVideoQualityUseCase(videoUploadQuality)
        setChargingRequiredForVideoCompressionUseCase(true)
        setVideoCompressionSizeLimitUseCase(videoCompressionSizeLimit)
        settingsRepository.setEnableCameraUpload(true)
        listenToNewMediaUseCase()
    }
}