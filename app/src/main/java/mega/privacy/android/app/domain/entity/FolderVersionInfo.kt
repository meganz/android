package mega.privacy.android.app.domain.entity

/**
 * Folder version info
 *
 * @property numberOfVersions
 * @property sizeOfPreviousVersionsInBytes
 */
data class FolderVersionInfo(val numberOfVersions: Int, val sizeOfPreviousVersionsInBytes: Long)
