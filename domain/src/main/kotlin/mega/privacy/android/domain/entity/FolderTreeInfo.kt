package mega.privacy.android.domain.entity

/**
 * Encapsulates folder info related to the entire tree of this folder and its sub-folders
 *
 * @property numberOfFiles the number of files this folder and its sub-folders has
 * @property numberOfFolders the number of folders and sub-folders this folder has, including itself
 * @property totalCurrentSizeInBytes the size of the folder without versions of contained files
 * @property numberOfVersions the total amount of versions files in this folder and its sub-folders have
 * @property sizeOfPreviousVersionsInBytes the size of the versions contained in this folder and its sub-folders
 */
data class FolderTreeInfo(
    val numberOfFiles: Int,
    val numberOfFolders: Int,
    val totalCurrentSizeInBytes: Long,
    val numberOfVersions: Int,
    val sizeOfPreviousVersionsInBytes: Long,
)
