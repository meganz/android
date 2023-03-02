package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.repository.SettingsRepository
import mega.privacy.android.domain.usecase.camerauploads.SetUploadVideoQuality
import javax.inject.Inject

/**
 * Default implementation of [EnablePhotosCameraUpload]
 *
 * @property settingsRepository [SettingsRepository]
 * @property setUploadVideoQuality [SetUploadVideoQuality]
 */
class DefaultEnablePhotosCameraUpload @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val setUploadVideoQuality: SetUploadVideoQuality,
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
            setCamSyncWifi(!enableCellularSync)
            setCameraUploadFileType(syncVideo)
            setCameraFolderExternalSDCard(false)
            setUploadVideoQuality(videoQuality)
            setConversionOnCharging(true)
            setChargingOnSize(conversionChargingOnSize)
            // After target and local folder setup, then enable CU.
            setEnableCameraUpload(true)
        }
    }
}
