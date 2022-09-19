package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

class DefaultEnablePhotosCameraUpload @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : EnablePhotosCameraUpload {

    override suspend fun invoke(
        path: String?,
        syncVideo: Boolean,
        enableCellularSync: Boolean,
        videoQuality: Int,
        conversionChargingOnSize: Int,
    ) {
        settingsRepository.setCameraUploadLocalPath(path)
        settingsRepository.setCamSyncWifi(!enableCellularSync)
        settingsRepository.setCameraUploadFileType(syncVideo)
        settingsRepository.setCameraFolderExternalSDCard(false)
        settingsRepository.setCameraUploadVideoQuality(videoQuality)
        settingsRepository.setConversionOnCharging(true)
        settingsRepository.setChargingOnSize(conversionChargingOnSize)
        // After target and local folder setup, then enable CU.
        settingsRepository.setEnableCameraUpload(true)
    }
}
