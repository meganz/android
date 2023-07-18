package mega.privacy.android.feature.devicecenter.data.mapper

import mega.privacy.android.feature.devicecenter.data.entity.BackupInfo
import nz.mega.sdk.MegaBackupInfo
import javax.inject.Inject

/**
 * Mapper that converts a [MegaBackupInfo] into [BackupInfo]
 *
 * @param backupInfoTypeMapper [BackupInfoTypeMapper]
 * @param backupInfoStateMapper [BackupInfoStateMapper]
 * @param backupInfoSubStateMapper [BackupInfoSubStateMapper]
 * @param backupInfoHeartbeatStatusMapper [BackupInfoHeartbeatStatusMapper]
 */
internal class BackupInfoMapper @Inject constructor(
    private val backupInfoTypeMapper: BackupInfoTypeMapper,
    private val backupInfoStateMapper: BackupInfoStateMapper,
    private val backupInfoSubStateMapper: BackupInfoSubStateMapper,
    private val backupInfoHeartbeatStatusMapper: BackupInfoHeartbeatStatusMapper,
) {

    /**
     * Invocation function
     *
     * @param sdkBackupInfo The [MegaBackupInfo]
     * @return The [BackupInfo] object
     */
    operator fun invoke(sdkBackupInfo: MegaBackupInfo) = BackupInfo(
        id = sdkBackupInfo.id(),
        type = backupInfoTypeMapper(sdkBackupInfo.type()),
        rootHandle = sdkBackupInfo.root(),
        localFolderPath = sdkBackupInfo.localFolder(),
        deviceId = sdkBackupInfo.deviceId(),
        state = backupInfoStateMapper(sdkBackupInfo.state()),
        subState = backupInfoSubStateMapper(sdkBackupInfo.substate()),
        extraInfo = sdkBackupInfo.extra(),
        name = sdkBackupInfo.name(),
        timestamp = sdkBackupInfo.ts(),
        status = backupInfoHeartbeatStatusMapper(sdkBackupInfo.status()),
        progress = sdkBackupInfo.progress(),
        uploadCount = sdkBackupInfo.uploads(),
        downloadCount = sdkBackupInfo.downloads(),
        lastActivityTimestamp = sdkBackupInfo.activityTs(),
        lastSyncedNodeHandle = sdkBackupInfo.lastSync(),
    )
}