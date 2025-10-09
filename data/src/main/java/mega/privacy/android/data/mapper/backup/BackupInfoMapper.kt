package mega.privacy.android.data.mapper.backup

import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.domain.entity.backup.BackupInfo
import mega.privacy.android.domain.entity.node.NodeId
import nz.mega.sdk.MegaBackupInfo
import javax.inject.Inject

/**
 * Mapper that converts a [MegaBackupInfo] into [BackupInfo]
 *
 * @property backupInfoHeartbeatStatusMapper [BackupInfoHeartbeatStatusMapper]
 * @property backupInfoStateMapper [BackupInfoStateMapper]
 * @property syncErrorMapper [SyncErrorMapper]
 * @property backupInfoTypeMapper [BackupInfoTypeMapper]
 * @property backupInfoUserAgentMapper [BackupInfoUserAgentMapper]
 */
internal class BackupInfoMapper @Inject constructor(
    private val backupInfoHeartbeatStatusMapper: BackupInfoHeartbeatStatusMapper,
    private val backupInfoStateMapper: BackupInfoStateMapper,
    private val syncErrorMapper: SyncErrorMapper,
    private val backupInfoTypeMapper: BackupInfoTypeMapper,
    private val backupInfoUserAgentMapper: BackupInfoUserAgentMapper,
    private val megaApiGateway: MegaApiGateway,
) {

    /**
     * Invocation function
     *
     * @param sdkBackupInfo A potentially nullable [MegaBackupInfo]
     * @return A potentially nullable [BackupInfo] object
     */
    suspend operator fun invoke(sdkBackupInfo: MegaBackupInfo?) =
        sdkBackupInfo?.let { megaBackupInfo ->
            val node = megaApiGateway.getMegaNodeByHandle(megaBackupInfo.root())
            if (node == null) return@let null
            BackupInfo(
                id = megaBackupInfo.id(),
                type = backupInfoTypeMapper(megaBackupInfo.type()),
                rootHandle = NodeId(megaBackupInfo.root()),
                localFolderPath = megaBackupInfo.localFolder(),
                deviceId = megaBackupInfo.deviceId(),
                userAgent = backupInfoUserAgentMapper(megaBackupInfo.deviceUserAgent()),
                state = backupInfoStateMapper(megaBackupInfo.state()),
                subState = syncErrorMapper(megaBackupInfo.substate()),
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
