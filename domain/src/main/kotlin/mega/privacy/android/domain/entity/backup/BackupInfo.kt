package mega.privacy.android.domain.entity.backup

/**
 * Data class representing the User's Backup information from the "sf" API Call. This class is
 * mapped from nz.mega.sdk.MegaBackupInfo
 *
 * @param id The Backup ID
 * @param type Any of the [BackupInfoType] values
 * @param rootHandle The Backup Root Handle
 * @param localFolderPath The name of the backed up Local Folder
 * @param deviceId The Device ID where the Backup originated
 * @param state Any of the [BackupInfoState] values
 * @param subState Any of the [BackupInfoSubState] values
 * @param extraInfo The extra information used as a source for extracting other details
 * @param name The Backup Name
 * @param timestamp The Backup Timestamp reported by Heartbeats, measured in seconds
 * @param status Any of the [BackupInfoHeartbeatStatus] values
 * @param progress The Backup Progress reported by Heartbeats
 * @param uploadCount The total Upload count
 * @param downloadCount The total Download count
 * @param lastActivityTimestamp The Last Activity Timestamp reported by Heartbeats, measured in seconds
 * @param lastSyncedNodeHandle The Last Synced Node Handle
 */
data class BackupInfo(
    val id: Long,
    val type: BackupInfoType,
    val rootHandle: Long,
    val localFolderPath: String?,
    val deviceId: String?,
    val state: BackupInfoState,
    val subState: BackupInfoSubState,
    val extraInfo: String?,
    val name: String?,
    val timestamp: Long,
    val status: BackupInfoHeartbeatStatus,
    val progress: Int,
    val uploadCount: Int,
    val downloadCount: Int,
    val lastActivityTimestamp: Long,
    val lastSyncedNodeHandle: Long,
)