package mega.privacy.android.feature.devicecenter.data.mapper

import mega.privacy.android.domain.entity.backup.BackupInfo
import mega.privacy.android.domain.entity.backup.BackupInfoHeartbeatStatus
import mega.privacy.android.domain.entity.backup.BackupInfoState
import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.entity.sync.SyncError
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceFolderStatus
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceFolderNode
import nz.mega.sdk.MegaApiJava
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Mapper that converts a list of [BackupInfo] objects into a list of [DeviceFolderNode] objects
 */
internal class DeviceFolderNodeMapper @Inject constructor() {

    /**
     * Invocation function
     *
     * @param backupInfoList A list of [BackupInfo] objects
     * @return The corresponding list of [DeviceFolderNode] objects
     */
    operator fun invoke(backupInfoList: List<BackupInfo>): List<DeviceFolderNode> {
        val currentTimeInSeconds = System.currentTimeMillis() / 1000L
        return backupInfoList.map { backupInfo ->
            DeviceFolderNode(
                id = backupInfo.id.toString(),
                name = backupInfo.name.orEmpty(),
                status = backupInfo.getDeviceFolderStatus(currentTimeInSeconds),
                rootHandle = backupInfo.rootHandle,
                localFolderPath = backupInfo.localFolderPath.orEmpty(),
                type = backupInfo.type,
                userAgent = backupInfo.userAgent,
            )
        }
    }

    /**
     * Returns the Folder Status of a Backup
     *
     * The order for detecting the appropriate Folder Status is
     * 1. Inactive - [isInactive]
     * 2. Stopped - [isStopped]
     * 3. Overquota - [isOverquota]
     * 4. Blocked - [isBlocked]
     * 5. Failed - [isFailed]
     * 6. Stalled - [isStalled]
     * 7. Disabled - [isDisabled]
     * 8. Offline - [isOffline]
     * 9. Paused - [isPaused]
     * 10. Initializing - [isInitializing]
     * 11. Syncing - [isSyncing]
     * 12. Scanning - [isScanning]
     * 13 Up to Date - [isUpToDate]
     *
     * If there is no matching Folder Status, [DeviceFolderStatus.Unknown] is returned
     *
     * @param currentTimeInSeconds The current time the [DeviceFolderNodeMapper] was invoked in
     * seconds
     * @return The Folder Status
     */
    private fun BackupInfo.getDeviceFolderStatus(currentTimeInSeconds: Long) = when {
        isInactive(currentTimeInSeconds) -> DeviceFolderStatus.Inactive

        isStopped() || isOverquota() || isBlocked() || isFailed() || isStalled() ->
            DeviceFolderStatus.Error(subState)

        isDisabled() -> {
            when (type) {
                BackupInfoType.CAMERA_UPLOADS, BackupInfoType.MEDIA_UPLOADS -> DeviceFolderStatus.Disabled
                else -> DeviceFolderStatus.Error(subState)
            }
        }

        isOffline(currentTimeInSeconds) -> DeviceFolderStatus.Error(subState)

        isPaused() -> {
            when (type) {
                BackupInfoType.CAMERA_UPLOADS, BackupInfoType.MEDIA_UPLOADS -> DeviceFolderStatus.Disabled
                else -> DeviceFolderStatus.Paused
            }
        }

        isInitializing() || isSyncing() || isScanning() -> DeviceFolderStatus.Updating(progress)

        isUpToDate() -> DeviceFolderStatus.UpToDate
        else -> DeviceFolderStatus.Unknown
    }

    /**
     * Checks whether the Backup is Inactive or not
     *
     * A Backup is Inactive if the last Heartbeat was more than 60 days ago
     *
     * @param currentTimeInSeconds The current time the [DeviceFolderNodeMapper] was invoked in seconds
     * @return true if the following conditions are met, and false if otherwise
     */
    private fun BackupInfo.isInactive(currentTimeInSeconds: Long): Boolean {
        val maxLastHeartbeatTime = TimeUnit.DAYS.toSeconds(60)
        val lastHeartbeat = maxOf(timestamp, lastActivityTimestamp)
        // How much time has passed since the last Heartbeat
        val heartbeatTimeDifference = currentTimeInSeconds - lastHeartbeat
        val isLastHeartbeatOutOfRange = heartbeatTimeDifference > maxLastHeartbeatTime
        return isLastHeartbeatOutOfRange
    }

    /**
     * Checks whether the Backup is Stopped or not
     *
     * @return true if the following conditions are met, and false if otherwise
     */
    private fun BackupInfo.isStopped() = state == BackupInfoState.NOT_INITIALIZED

    /**
     * Checks whether the Backup is Overquota or not
     *
     * @return true if the following conditions are met, and false if otherwise
     */
    private fun BackupInfo.isOverquota() = state in listOf(
        BackupInfoState.FAILED,
        BackupInfoState.TEMPORARY_DISABLED,
    ) && subState == SyncError.STORAGE_OVERQUOTA

    /**
     * Checks whether the Backup is Blocked or not
     *
     * @return true if the following conditions are met, and false if otherwise
     */
    private fun BackupInfo.isBlocked() = state in listOf(
        BackupInfoState.FAILED,
        BackupInfoState.TEMPORARY_DISABLED,
    ) && subState in listOf(
        SyncError.ACCOUNT_EXPIRED,
        SyncError.ACCOUNT_BLOCKED,
        SyncError.NO_SYNC_ERROR
    )

    /**
     * Checks whether the Backup has Failed or not
     *
     * @return true if the following conditions are met, and false if otherwise
     */
    private fun BackupInfo.isFailed() = state in listOf(
        BackupInfoState.FAILED,
        BackupInfoState.TEMPORARY_DISABLED,
    ) && subState !in listOf(
        SyncError.ACCOUNT_EXPIRED,
        SyncError.ACCOUNT_BLOCKED,
        SyncError.NO_SYNC_ERROR,
        SyncError.STORAGE_OVERQUOTA
    )

    /**
     * Checks whether the Backup is Stalled or not, which is only applied for Syncs
     *
     * @return true if the following conditions are met, and false if otherwise
     */
    private fun BackupInfo.isStalled() = status == BackupInfoHeartbeatStatus.STALLED

    /**
     * Checks whether the Backup is Disabled or not
     *
     * @return true if the following conditions are met, and false if otherwise
     */
    private fun BackupInfo.isDisabled() = state == BackupInfoState.DISABLED

    /**
     * Checks whether the Backup is Offline or not
     *
     * A Backup is Offline when the following conditions are met:
     * 1. No Heartbeat was found
     * 2. For Mobile Devices, if the last Camera Uploads Heartbeat was more than 60 minutes ago
     * 3. For Other Devices, if the last Backup Heartbeat was more than 30 minutes ago
     * 4. The MEGA Folder is missing OR created more than 10 minutes ago
     *
     * @param currentTimeInSeconds The current time the [DeviceFolderNodeMapper] was invoked in seconds
     * @return true if the following conditions are met, and false if otherwise
     */
    private fun BackupInfo.isOffline(currentTimeInSeconds: Long): Boolean {
        val maxLastHeartbeatTimeForMobileDevices = TimeUnit.MINUTES.toSeconds(60)
        val maxLastSyncTimeForOtherDevices = TimeUnit.MINUTES.toSeconds(30)
        val maxCreatedBackupTime = TimeUnit.MINUTES.toSeconds(10)

        val isMobileBackup =
            type == BackupInfoType.CAMERA_UPLOADS || type == BackupInfoType.MEDIA_UPLOADS
        val lastBackupHeartbeat = maxOf(timestamp, lastActivityTimestamp)
        // How much time has passed since the last Heartbeat
        val backupHeartbeatTimeDifference = currentTimeInSeconds - lastBackupHeartbeat
        val isLastBackupHeartbeatOutOfRange =
            (isMobileBackup && (backupHeartbeatTimeDifference > maxLastHeartbeatTimeForMobileDevices))
                    || (!isMobileBackup && (backupHeartbeatTimeDifference > maxLastSyncTimeForOtherDevices))

        return if (isLastBackupHeartbeatOutOfRange) {
            // How much time has passed since the Backup was created
            val createdBackupTimeDifference = currentTimeInSeconds - timestamp
            val isBackupOld = createdBackupTimeDifference > maxCreatedBackupTime
            val isCurrentBackupFolderExisting = rootHandle.longValue != MegaApiJava.INVALID_HANDLE

            isCurrentBackupFolderExisting || isBackupOld
        } else {
            false
        }
    }

    /**
     * Checks whether the Backup is Paused or not
     *
     * @return true if the following conditions are met, and false if otherwise
     */
    private fun BackupInfo.isPaused() =
        this.isTwoWaySyncPaused() || this.isUploadSyncPaused() || this.isDownloadSyncPaused()

    /**
     * Checks whether a Backup on a Two-Way Sync is Paused or not
     *
     * @return true if the following conditions are met, and false if otherwise
     */
    private fun BackupInfo.isTwoWaySyncPaused() =
        type == BackupInfoType.TWO_WAY_SYNC && state in listOf(
            BackupInfoState.PAUSE_UP,
            BackupInfoState.PAUSE_DOWN,
            BackupInfoState.PAUSE_FULL,
            BackupInfoState.DELETED,
        )

    /**
     * Checks whether a Backup on an Upload Sync is Paused or not
     *
     * @return true if the following conditions are met, and false if otherwise
     */
    private fun BackupInfo.isUploadSyncPaused() =
        type in listOf(
            BackupInfoType.UP_SYNC,
            BackupInfoType.CAMERA_UPLOADS,
            BackupInfoType.MEDIA_UPLOADS,
            BackupInfoType.BACKUP_UPLOAD,
        ) && state in listOf(BackupInfoState.PAUSE_UP, BackupInfoState.PAUSE_FULL)

    /**
     * Checks whether a Backup on a Download Sync is Paused or not
     *
     * @return true if the following conditions are met, and false if otherwise
     */
    private fun BackupInfo.isDownloadSyncPaused() =
        type == BackupInfoType.DOWN_SYNC && state in listOf(
            BackupInfoState.PAUSE_DOWN,
            BackupInfoState.PAUSE_FULL,
            BackupInfoState.DELETED,
        )

    /**
     * Checks whether the Backup is Up to Date or not
     *
     * @return true if the following conditions are met, and false if otherwise
     */
    private fun BackupInfo.isUpToDate() = state in listOf(
        BackupInfoState.ACTIVE,
        BackupInfoState.PAUSE_UP,
        BackupInfoState.PAUSE_DOWN,
        BackupInfoState.PAUSE_FULL,
        BackupInfoState.DELETED,
    ) && status in listOf(BackupInfoHeartbeatStatus.UPTODATE, BackupInfoHeartbeatStatus.INACTIVE)

    /**
     * Checks whether the Backup is Initializing or not
     *
     * @return true if the following conditions are met, and false if otherwise
     */
    private fun BackupInfo.isInitializing() = status == BackupInfoHeartbeatStatus.UNKNOWN

    /**
     * Checks whether the Backup is Syncing
     *
     * @return true if the following conditions are met, and false if otherwise
     */
    private fun BackupInfo.isSyncing() = status == BackupInfoHeartbeatStatus.SYNCING

    /**
     * Checks whether the Backup is Scanning or not
     *
     * @return true if the following conditions are met, and false if otherwise
     */
    private fun BackupInfo.isScanning() = status == BackupInfoHeartbeatStatus.PENDING
}
