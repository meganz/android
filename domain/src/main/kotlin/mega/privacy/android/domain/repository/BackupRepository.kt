package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.backup.Backup
import mega.privacy.android.domain.entity.backup.BackupInfo

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
     * @param state [Int]
     * @param subState [Int]
     */
    suspend fun setBackup(
        backupType: Int, targetNode: Long, localFolder: String, backupName: String,
        state: Int, subState: Int,
    ): Backup
}
