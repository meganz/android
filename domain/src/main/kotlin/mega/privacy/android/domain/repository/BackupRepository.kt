package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.BackupState
import mega.privacy.android.domain.entity.backup.Backup
import mega.privacy.android.domain.entity.backup.BackupInfo
import mega.privacy.android.domain.entity.backup.BackupInfoType

/**
 * Repository class that provides several functions for Backup-related operations
 */
interface BackupRepository {

    /**
     * Retrieves the Device ID of the current Device
     *
     * @return the Device ID
     */
    suspend fun getDeviceId(): String?

    /**
     * Retrieves User information on the list of backed up Devices, represented as a [Map].
     * Each [Map] entry represents a Key-Value Pair of a Device ID and Device Name, respectively
     *
     * @return A [Map] whose Key-Value Pair consists of the Device ID and Device Name
     */
    suspend fun getDeviceIdAndNameMap(): Map<String, String>

    /**
     * Get the Device Name from Server
     *
     * @param deviceId The Device ID identifying the Device
     * @return [String] Device Name
     */
    suspend fun getDeviceName(deviceId: String): String?

    /**
     * Renames a Device
     *
     * @param deviceId The Device ID identifying the Device to be renamed
     * @param deviceName The new Device Name
     */
    suspend fun renameDevice(deviceId: String, deviceName: String)

    /**
     * Retrieves all of the User's Backup information
     *
     * @return A list of [BackupInfo] objects
     */
    suspend fun getBackupInfo(): List<BackupInfo>

    /**
     * Registers a backup to display in Device Centre
     * @param backupType [Int]
     * @param targetNode [Long]
     * @param localFolder [String]
     * @param backupName [String]
     * @param state [BackupState]
     */
    suspend fun setBackup(
        backupType: BackupInfoType, targetNode: Long, localFolder: String, backupName: String,
        state: BackupState,
    ): Backup


    /**
     * Updates a backup to display in Device Centre
     * Any values that should not be changed should be marked as null, -1 or -1L
     * @param backupId [Long]
     * @param backupType [BackupInfoType]
     * @param backupName [String]
     * @param targetNode [Long]
     * @param localFolder [String]
     * @param state [BackupState]
     */
    suspend fun updateRemoteBackup(
        backupId: Long, backupType: BackupInfoType, backupName: String, targetNode: Long,
        localFolder: String?, state: BackupState,
    )

    /**
     * Monitor BackupInfoType.
     *
     * @return Flow [BackupInfoType]
     */
    fun monitorBackupInfoType(): Flow<BackupInfoType>

    /**
     * Broadcast BackupInfoType.
     *
     * @param backupInfoType [BackupInfoType]
     */
    suspend fun broadCastBackupInfoType(backupInfoType: BackupInfoType)

    /**
     * Save a backup to Database
     *
     * @param backup [Backup]
     */
    suspend fun saveBackup(backup: Backup)
}
