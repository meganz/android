package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.FolderVersionInfo

/**
 * Get folder version info
 *
 */
fun interface GetFolderVersionInfo {
    /**
     * Invoke
     *
     * @return
     */
    suspend operator fun invoke(): FolderVersionInfo?
}