package mega.privacy.android.app.domain.repository

import mega.privacy.android.app.domain.entity.FolderVersionInfo

/**
 * Files repository
 *
 */
interface FilesRepository {
    /**
     * Get folder version info
     *
     * @return info
     */
    suspend fun getFolderVersionInfo(): FolderVersionInfo
}
