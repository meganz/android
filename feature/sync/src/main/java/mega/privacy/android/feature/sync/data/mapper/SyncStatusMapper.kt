package mega.privacy.android.feature.sync.data.mapper

import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsFinishedReason
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsStatusInfo
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import nz.mega.sdk.MegaSync.SyncRunningState
import nz.mega.sdk.MegaSyncStats
import javax.inject.Inject

/**
 * This class handles mapping Sync Status based on SyncStats and RunningState.
 *
 * Currently the mapping to Error state is not implemented. It will be implemented with Stall Issues
 */
internal class SyncStatusMapper @Inject constructor() {

    operator fun invoke(
        syncStats: MegaSyncStats?,
        runningState: Int,
    ): SyncStatus = when {
        runningState == SyncRunningState.RUNSTATE_SUSPENDED.swigValue() -> SyncStatus.PAUSED
        syncStats == null -> SyncStatus.SYNCED
        syncStats.isScanning || syncStats.isSyncing -> SyncStatus.SYNCING
        else -> SyncStatus.SYNCED
    }

    operator fun invoke(
        backupState: BackupState,
        backupType: BackupInfoType,
        cuStatusInfo: CameraUploadsStatusInfo? = null,
    ): SyncStatus = when (backupState) {
        BackupState.ACTIVE -> {
            cuStatusInfo?.let {
                when (cuStatusInfo) {
                    CameraUploadsStatusInfo.Unknown -> SyncStatus.SYNCED

                    CameraUploadsStatusInfo.Started,
                    CameraUploadsStatusInfo.CheckFilesForUpload,
                    is CameraUploadsStatusInfo.UploadProgress,
                    is CameraUploadsStatusInfo.VideoCompressionProgress,
                    CameraUploadsStatusInfo.VideoCompressionSuccess,
                        -> SyncStatus.SYNCING

                    CameraUploadsStatusInfo.VideoCompressionOutOfSpace,
                    CameraUploadsStatusInfo.VideoCompressionError,
                    CameraUploadsStatusInfo.StorageOverQuota,
                    CameraUploadsStatusInfo.NotEnoughStorage,
                        -> SyncStatus.ERROR

                    CameraUploadsStatusInfo.FolderUnavailable(CameraUploadFolderType.Primary) ->
                        if (backupType == BackupInfoType.CAMERA_UPLOADS) SyncStatus.ERROR else SyncStatus.PAUSED

                    CameraUploadsStatusInfo.FolderUnavailable(CameraUploadFolderType.Secondary) ->
                        if (backupType == BackupInfoType.MEDIA_UPLOADS) SyncStatus.ERROR else SyncStatus.PAUSED

                    CameraUploadsStatusInfo.Finished(CameraUploadsFinishedReason.DISABLED) -> SyncStatus.DISABLED

                    CameraUploadsStatusInfo.Finished(CameraUploadsFinishedReason.LOGIN_FAILED),
                    CameraUploadsStatusInfo.Finished(CameraUploadsFinishedReason.LOCAL_PRIMARY_FOLDER_NOT_VALID),
                    CameraUploadsStatusInfo.Finished(CameraUploadsFinishedReason.MEDIA_PERMISSION_NOT_GRANTED),
                    CameraUploadsStatusInfo.Finished(CameraUploadsFinishedReason.TARGET_NODES_DELETED),
                    CameraUploadsStatusInfo.Finished(CameraUploadsFinishedReason.INSUFFICIENT_LOCAL_STORAGE_SPACE),
                    CameraUploadsStatusInfo.Finished(CameraUploadsFinishedReason.ERROR_DURING_PROCESS),
                        -> SyncStatus.ERROR

                    CameraUploadsStatusInfo.Finished(CameraUploadsFinishedReason.BATTERY_LEVEL_TOO_LOW),
                    CameraUploadsStatusInfo.Finished(CameraUploadsFinishedReason.NETWORK_CONNECTION_REQUIREMENT_NOT_MET),
                    CameraUploadsStatusInfo.Finished(CameraUploadsFinishedReason.ACCOUNT_STORAGE_OVER_QUOTA),
                    CameraUploadsStatusInfo.Finished(CameraUploadsFinishedReason.DEVICE_CHARGING_REQUIREMENT_NOT_MET),
                        -> SyncStatus.PAUSED

                    else -> SyncStatus.SYNCED
                }
            } ?: SyncStatus.SYNCED
        }

        BackupState.PAUSE_UPLOADS, BackupState.PAUSE_DOWNLOADS, BackupState.PAUSE_ALL -> SyncStatus.PAUSED
        BackupState.NOT_INITIALIZED, BackupState.TEMPORARILY_DISABLED, BackupState.DISABLED -> SyncStatus.DISABLED
        BackupState.INVALID, BackupState.DELETED, BackupState.FAILED -> SyncStatus.ERROR
        else -> SyncStatus.SYNCING
    }
}
