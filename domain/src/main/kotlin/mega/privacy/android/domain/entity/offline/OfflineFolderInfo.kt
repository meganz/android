package mega.privacy.android.domain.entity.offline

/**
 * Offline Folder Information
 *
 * @property numFolders Number of folders inside the folder
 * @property numFiles Number of files inside the folder
 */
data class OfflineFolderInfo(
    val numFolders: Int,
    val numFiles: Int,
)
