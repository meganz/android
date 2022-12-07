package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Default enable photos camera upload
 *
 * @property settingsRepository
 */
class DefaultEnablePhotosCameraUpload @Inject constructor(
    private val settingsRepository: SettingsRepository,
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
            setCameraUploadVideoQuality(videoQuality)
            setConversionOnCharging(true)
            setChargingOnSize(conversionChargingOnSize)
            // After target and local folder setup, then enable CU.
            setEnableCameraUpload(true)
        }
    }
}
