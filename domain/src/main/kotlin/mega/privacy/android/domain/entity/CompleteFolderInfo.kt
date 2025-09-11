package mega.privacy.android.domain.entity

/**
 * Complete folder information combining node properties and tree statistics
 *
 * @property numOfFiles Number of files in the folder and its sub-folders
 * @property numOfFolders Number of folders in the folder and its sub-folders
 * @property totalSizeInBytes Total size of files in the folder and its sub-folders
 * @property creationTime Creation time of the folder
 */
data class CompleteFolderInfo(
    val numOfFiles: Int,
    val numOfFolders: Int,
    val totalSizeInBytes: Long,
    val creationTime: Long,
)
