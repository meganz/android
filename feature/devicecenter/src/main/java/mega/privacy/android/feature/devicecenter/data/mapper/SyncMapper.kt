package mega.privacy.android.feature.devicecenter.data.mapper

import mega.privacy.android.feature.devicecenter.data.entity.Sync
import nz.mega.sdk.MegaBackupInfo
import javax.inject.Inject

/**
 * Mapper that converts a [MegaBackupInfo] into [Sync]
 *
 * @param syncTypeMapper [SyncTypeMapper]
 * @param syncStateMapper [SyncStateMapper]
 * @param syncSubStateMapper [SyncSubStateMapper]
 * @param syncStatusMapper [SyncStatusMapper]
 */
internal class SyncMapper @Inject constructor(
    private val syncTypeMapper: SyncTypeMapper,
    private val syncStateMapper: SyncStateMapper,
    private val syncSubStateMapper: SyncSubStateMapper,
    private val syncStatusMapper: SyncStatusMapper,
) {

    /**
     * Invocation function
     *
     * @param sdkBackupInfo The [MegaBackupInfo]
     * @return The [Sync] object
     */
    operator fun invoke(sdkBackupInfo: MegaBackupInfo) = Sync(
        id = sdkBackupInfo.id(),
        type = syncTypeMapper(sdkBackupInfo.type()),
        rootHandle = sdkBackupInfo.root(),
        localFolderPath = sdkBackupInfo.localFolder(),
        deviceId = sdkBackupInfo.deviceId(),
        state = syncStateMapper(sdkBackupInfo.state()),
        subState = syncSubStateMapper(sdkBackupInfo.substate()),
        extraInfo = sdkBackupInfo.extra(),
        name = sdkBackupInfo.name(),
        timestamp = sdkBackupInfo.ts(),
        status = syncStatusMapper(sdkBackupInfo.status()),
        progress = sdkBackupInfo.progress(),
        uploadCount = sdkBackupInfo.uploads(),
        downloadCount = sdkBackupInfo.downloads(),
        lastActivityTimestamp = sdkBackupInfo.activityTs(),
        lastSyncedNodeHandle = sdkBackupInfo.lastSync(),
    )
}