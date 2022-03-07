package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.entity.FolderVersionInfo

/**
 * Get folder version info
 *
 */
interface GetFolderVersionInfo {
    /**
     * Invoke
     *
     * @return
     */
    suspend operator fun invoke(): FolderVersionInfo?
}