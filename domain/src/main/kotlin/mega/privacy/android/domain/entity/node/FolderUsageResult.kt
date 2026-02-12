package mega.privacy.android.domain.entity.node

/**
 * Represents the usage status of a folder across Camera Uploads, Sync, and Backup features.
 */
sealed interface FolderUsageResult {
    /**
     * Folder is not used by any feature
     */
    data object NotUsed : FolderUsageResult

    /**
     * Folder is exactly matched with Camera Uploads folder
     */
    data object UsedByCameraUpload : FolderUsageResult

    /**
     * Folder is a parent of Camera Uploads folder (contains Camera Uploads folder)
     */
    data object UsedByCameraUploadParent : FolderUsageResult

    /**
     * Folder is a child of Camera Uploads folder (inside Camera Uploads folder)
     */
    data object UsedByCameraUploadChild : FolderUsageResult

    /**
     * Folder is exactly matched with Media Uploads folder
     */
    data object UsedByMediaUpload : FolderUsageResult

    /**
     * Folder is a parent of Media Uploads folder (contains Media Uploads folder)
     */
    data object UsedByMediaUploadParent : FolderUsageResult

    /**
     * Folder is a child of Media Uploads folder (inside Media Uploads folder)
     */
    data object UsedByMediaUploadChild : FolderUsageResult

    /**
     * Folder is exactly matched with a Sync or Backup folder
     * @param deviceId The device ID of the sync/backup, null if unknown
     */
    data class UsedBySyncOrBackup(val deviceId: String?) : FolderUsageResult

    /**
     * Folder is a parent of a Sync or Backup folder (contains a synced/backed up folder)
     * @param deviceId The device ID of the sync/backup, null if unknown
     */
    data class UsedBySyncOrBackupParent(val deviceId: String?) : FolderUsageResult

    /**
     * Folder is a child of a Sync or Backup folder (inside a synced/backed up folder)
     * @param deviceId The device ID of the sync/backup, null if unknown
     */
    data class UsedBySyncOrBackupChild(val deviceId: String?) : FolderUsageResult
}
