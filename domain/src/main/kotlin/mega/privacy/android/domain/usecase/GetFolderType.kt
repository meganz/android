package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.FolderType
import mega.privacy.android.domain.entity.node.FolderNode

/**
 * Get folder location information
 */
fun interface GetFolderType {
    /**
     * Invoke
     *
     * @param folder
     * @return folder location information
     */
    suspend operator fun invoke(folder: FolderNode): FolderType
}