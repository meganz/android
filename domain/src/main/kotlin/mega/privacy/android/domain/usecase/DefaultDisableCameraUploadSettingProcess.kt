package mega.privacy.android.domain.usecase

import kotlinx.coroutines.delay
import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.repository.CacheFileRepository
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Default Implementation of DisableCameraUploadSettingProcess
 *
 */
class DefaultDisableCameraUploadSettingProcess @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val cacheFileRepository: CacheFileRepository,
    private val settingsRepository: SettingsRepository,
) : DisableCameraUploadSettingProcess {

    override suspend fun invoke(clearCamSyncRecords: Boolean) {
        cameraUploadRepository.resetCUTimestamps(clearCamSyncRecords)
        cacheFileRepository.purgeCacheDirectory()
        delay(10 * 1000)
        if (cameraUploadRepository.shouldClearSyncRecords()) {
            cameraUploadRepository.deleteAllSyncRecords(SyncRecordType.TYPE_ANY.value)
            cameraUploadRepository.shouldClearSyncRecords(false)
        }
        settingsRepository.setEnableCameraUpload(false)
        cameraUploadRepository.setSecondaryEnabled(false)
    }
}