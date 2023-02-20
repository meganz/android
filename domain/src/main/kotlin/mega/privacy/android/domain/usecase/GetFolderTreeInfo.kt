package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.FolderTreeInfo
import mega.privacy.android.domain.entity.node.FolderNode

/**
 * Get folder version info
 *
 */
fun interface GetFolderTreeInfo {
    /**
     * Invoke
     * @param folderNode the [FolderNode] which [FolderTreeInfo] will be returned
     *
     * @return [FolderTreeInfo] of the required folder
     */
    suspend operator fun invoke(folderNode: FolderNode): FolderTreeInfo
}