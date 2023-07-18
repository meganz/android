package mega.privacy.android.feature.devicecenter.data.entity

/**
 * Data class representing information of a Sync mapped from [nz.mega.sdk.MegaBackupInfo]
 *
 * @param id The Sync ID
 * @param type Any of the [SyncType] values
 * @param rootHandle The Sync Root Handle
 * @param localFolderPath The name of the Synced Local Folder
 * @param deviceId The Device ID where the Sync originated
 * @param state Any of the [SyncState] values
 * @param subState Any of the [SyncSubState] values
 * @param extraInfo The extra information used as a source for extracting other details
 * @param name The Sync Name
 * @param timestamp The Sync Timestamp reported by Heartbeats
 * @param status Any of the [SyncStatus] values
 * @param progress The Sync Progress reported by Heartbeats
 * @param uploadCount The total Upload count
 * @param downloadCount The total Download count
 * @param lastActivityTimestamp The Last Activity Timestamp reported by Heartbeats
 * @param lastSyncedNodeHandle The Last Synced Node Handle
 */
data class Sync(
    val id: Long,
    val type: SyncType,
    val rootHandle: Long,
    val localFolderPath: String?,
    val deviceId: String?,
    val state: SyncState,
    val subState: SyncSubState,
    val extraInfo: String?,
    val name: String?,
    val timestamp: Long,
    val status: SyncStatus,
    val progress: Int,
    val uploadCount: Int,
    val downloadCount: Int,
    val lastActivityTimestamp: Long,
    val lastSyncedNodeHandle: Long,
)