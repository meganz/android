package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Default Implementation of DisableCameraUploadSettings
 *
 */
class DefaultDisableCameraUploadSettings @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val settingsRepository: SettingsRepository,
) : DisableCameraUploadSettings {

    override suspend fun invoke(clearCamSyncRecords: Boolean) {
        settingsRepository.setEnableCameraUpload(false)
        cameraUploadRepository.setSecondaryEnabled(false)
    }
}