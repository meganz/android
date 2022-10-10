package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.CameraUploadState
import javax.inject.Inject

/**
 * Default implementation of [CheckCameraUpload]
 */
class DefaultCheckCameraUpload @Inject constructor(
    private val isSecondaryFolderEnabled: IsSecondaryFolderEnabled,
    private val isNodeInRubbish: IsNodeInRubbish,
    private val backupTimeStampsAndFolderHandle: BackupTimeStampsAndFolderHandle,
    private val resetCameraUploadTimeStamps: ResetCameraUploadTimeStamps,
    private val clearCacheDirectory: ClearCacheDirectory,
    private val clearSyncRecords: ClearSyncRecords,
    private val resetMediaUploadTimeStamps: ResetMediaUploadTimeStamps,
    private val disableCameraUploadSettings: DisableCameraUploadSettings,
    private val disableMediaUploadSettings: DisableMediaUploadSettings,
) :
    CheckCameraUpload {
    override suspend fun invoke(
        shouldDisable: Boolean,
        primaryHandle: Long,
        secondaryHandle: Long,
    ): CameraUploadState {
        var result = CameraUploadState(shouldStopProcess = true, shouldSendEvent = false)
        val isSecondaryEnabled = isSecondaryFolderEnabled()
        val isPrimaryFolderInRubbish = isNodeInRubbish(primaryHandle)
        val isSecondaryFolderInRubbish = isSecondaryEnabled && isNodeInRubbish(secondaryHandle)
        if (isSecondaryFolderInRubbish && !isPrimaryFolderInRubbish) {
            if (shouldDisable) {
                // Back up timestamps
                backupTimeStampsAndFolderHandle()
                //Disable MU upload
                resetMediaUploadTimeStamps()
                disableMediaUploadSettings()
            }
            result = result.copy(shouldStopProcess = true)
        } else if (isPrimaryFolderInRubbish) {
            if (shouldDisable) {
                backupTimeStampsAndFolderHandle()
                resetCameraUploadTimeStamps(false)
                clearCacheDirectory()
                disableCameraUploadSettings(false)
                clearSyncRecords()
                result = result.copy(shouldSendEvent = true)
            }
            result = result.copy(shouldStopProcess = true)
        }
        return result
    }
}