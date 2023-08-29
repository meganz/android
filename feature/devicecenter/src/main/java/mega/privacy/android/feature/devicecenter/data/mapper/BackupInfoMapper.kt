package mega.privacy.android.feature.devicecenter.data.mapper

import mega.privacy.android.feature.devicecenter.domain.entity.BackupInfo
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
     * @param sdkBackupInfo A potentially nullable [MegaBackupInfo]
     * @return A potentially nullable [BackupInfo] object
     */
    operator fun invoke(sdkBackupInfo: MegaBackupInfo?) = sdkBackupInfo?.let { megaBackupInfo ->
        BackupInfo(
            id = megaBackupInfo.id(),
            type = backupInfoTypeMapper(megaBackupInfo.type()),
            rootHandle = megaBackupInfo.root(),
            localFolderPath = megaBackupInfo.localFolder(),
            deviceId = megaBackupInfo.deviceId(),
            state = backupInfoStateMapper(megaBackupInfo.state()),
            subState = backupInfoSubStateMapper(megaBackupInfo.substate()),
            extraInfo = megaBackupInfo.extra(),
            name = megaBackupInfo.name(),
            timestamp = megaBackupInfo.ts(),
            status = backupInfoHeartbeatStatusMapper(megaBackupInfo.status()),
            progress = megaBackupInfo.progress(),
            uploadCount = megaBackupInfo.uploads(),
            downloadCount = megaBackupInfo.downloads(),
            lastActivityTimestamp = megaBackupInfo.activityTs(),
            lastSyncedNodeHandle = megaBackupInfo.lastSync(),
        )
    }
}