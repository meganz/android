package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.CameraUploadState
import mega.privacy.android.domain.usecase.BackupTimeStampsAndFolderHandle
import mega.privacy.android.domain.usecase.ClearCacheDirectory
import mega.privacy.android.domain.usecase.ClearSyncRecords
import mega.privacy.android.domain.usecase.DisableMediaUploadSettings
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import mega.privacy.android.domain.usecase.ResetCameraUploadTimeStamps
import mega.privacy.android.domain.usecase.ResetMediaUploadTimeStamps
import javax.inject.Inject

/**
 * Check Camera Upload
 */
class AreCameraUploadsFoldersInRubbishBinUseCase @Inject constructor(
    private val isSecondaryFolderEnabled: IsSecondaryFolderEnabled,
    private val isNodeInRubbish: IsNodeInRubbish,
    private val backupTimeStampsAndFolderHandle: BackupTimeStampsAndFolderHandle,
    private val resetCameraUploadTimeStamps: ResetCameraUploadTimeStamps,
    private val clearCacheDirectory: ClearCacheDirectory,
    private val clearSyncRecords: ClearSyncRecords,
    private val resetMediaUploadTimeStamps: ResetMediaUploadTimeStamps,
    private val disableCameraUploadsSettingsUseCase: DisableCameraUploadsSettingsUseCase,
    private val disableMediaUploadSettings: DisableMediaUploadSettings,
) {

    /**
     * Invoke
     * @param shouldDisable whether to disable camera upload job or not
     * @param primaryHandle Primary Folder Handle
     * @param secondaryHandle Secondary Folder Handle
     * @return [CameraUploadState]
     */
    suspend operator fun invoke(
        shouldDisable: Boolean,
        primaryHandle: Long,
        secondaryHandle: Long,
    ): CameraUploadState {
        var result = CameraUploadState(shouldStopProcess = false, shouldSendEvent = false)
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
                disableCameraUploadsSettingsUseCase()
                clearSyncRecords()
                result = result.copy(shouldSendEvent = true)
            }
            result = result.copy(shouldStopProcess = true)
        }
        return result
    }
}
