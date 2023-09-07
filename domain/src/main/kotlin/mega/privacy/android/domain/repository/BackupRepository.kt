package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.backup.Backup

/**
 * Backup Repository
 */
interface BackupRepository {

    /**
     * Get the Device Name from Server
     *
     * @param deviceId The Device ID identifying the Device
     * @return [String] Device Name
     */
    suspend fun getDeviceName(deviceId: String): String?

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
