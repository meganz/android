package mega.privacy.android.feature.devicecenter.domain.mapper

import mega.privacy.android.feature.devicecenter.data.entity.BackupInfo
import mega.privacy.android.feature.devicecenter.data.entity.BackupInfoHeartbeatStatus
import mega.privacy.android.feature.devicecenter.data.entity.BackupInfoState
import mega.privacy.android.feature.devicecenter.data.entity.BackupInfoSubState
import mega.privacy.android.feature.devicecenter.data.entity.BackupInfoType
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceCenterNodeStatus
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceFolderNode
import nz.mega.sdk.MegaApiJava
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Mapper that converts a list of [BackupInfo] objects into a list of [DeviceFolderNode] objects
 */
class DeviceFolderNodeMapper @Inject constructor() {

    /**
     * Invocation function
     *
     * @param backupInfoList A list of [BackupInfo] objects
     * @return The corresponding list of [DeviceFolderNode] objects
     */
    operator fun invoke(backupInfoList: List<BackupInfo>): List<DeviceFolderNode> {
        val currentTime = System.currentTimeMillis()
        return backupInfoList.map { backupInfo ->
            DeviceFolderNode(
                id = backupInfo.id.toString(),
                name = backupInfo.name ?: "",
                status = backupInfo.getDeviceFolderStatus(currentTime),
            )
        }
    }

    /**
     * Returns the Folder Status of a Backup
     *
     * The order for detecting the appropriate Folder Status is
     * 1. Stopped - [isStopped]
     * 2. Overquota - [isOverquota]
     * 3. Blocked - [isBlocked]
     * 4. Disabled - [isDisabled]
     * 5. Offline - [isOffline]
     * 6. Paused - [isPaused]
     * 7. Up to Date - [isUpToDate]
     * 8. Initializing - [isInitializing]
     * 9. Syncing - [isSyncing]
     * 10. Scanning - [isScanning]
     *
     * If there is no matching Folder Status, [DeviceCenterNodeStatus.Unknown] is returned
     *
     * @param currentTime The current time the [DeviceFolderNodeMapper] was invoked, measured in
     * milliseconds
     * @return The Folder Status
     */
    private fun BackupInfo.getDeviceFolderStatus(currentTime: Long) = when {
        isStopped() -> DeviceCenterNodeStatus.Stopped
        isOverquota() -> DeviceCenterNodeStatus.Overquota
        isBlocked() -> DeviceCenterNodeStatus.Blocked(subState)
        isDisabled() -> DeviceCenterNodeStatus.Disabled
        isOffline(currentTime) -> DeviceCenterNodeStatus.Offline
        isPaused() -> DeviceCenterNodeStatus.Paused
        isUpToDate() -> DeviceCenterNodeStatus.UpToDate
        isInitializing() -> DeviceCenterNodeStatus.Initializing
        isSyncing() -> DeviceCenterNodeStatus.Syncing(progress)
        isScanning() -> DeviceCenterNodeStatus.Scanning
        else -> DeviceCenterNodeStatus.Unknown
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
    ) && subState == BackupInfoSubState.STORAGE_OVERQUOTA

    /**
     * Checks whether the Backup is Blocked or not
     *
     * @return true if the following conditions are met, and false if otherwise
     */
    private fun BackupInfo.isBlocked() = state in listOf(
        BackupInfoState.FAILED,
        BackupInfoState.TEMPORARY_DISABLED,
    ) && subState != BackupInfoSubState.STORAGE_OVERQUOTA

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
     * @param currentTime The current time the [DeviceFolderNodeMapper] was invoked, measured in
     * milliseconds
     * @return true if the following conditions are met, and false if otherwise
     */
    private fun BackupInfo.isOffline(currentTime: Long): Boolean {
        val maxLastHeartbeatTimeForMobileDevices = TimeUnit.MINUTES.toMillis(60)
        val maxLastSyncTimeForOtherDevices = TimeUnit.MINUTES.toMillis(30)
        val maxLastBackupType = TimeUnit.MINUTES.toMillis(10)

        val isMobileBackup =
            type == BackupInfoType.CAMERA_UPLOADS || type == BackupInfoType.MEDIA_UPLOADS
        val lastBackupTimestamp = currentTime - timestamp
        val lastBackupActivityTimestamp = currentTime - lastActivityTimestamp
        val lastBackupHeartbeat =
            currentTime - maxOf(lastBackupTimestamp, lastBackupActivityTimestamp)
        val isLastBackupHeartbeatOutOfRange =
            (isMobileBackup && (lastBackupHeartbeat > maxLastHeartbeatTimeForMobileDevices))
                    || (!isMobileBackup && (lastBackupHeartbeat > maxLastSyncTimeForOtherDevices))

        return if (isLastBackupHeartbeatOutOfRange) {
            val isBackupOld = lastBackupTimestamp > maxLastBackupType
            val isCurrentBackupFolderExisting = rootHandle != MegaApiJava.INVALID_HANDLE

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