package mega.privacy.android.domain.usecase.folderlink

import mega.privacy.android.domain.entity.folderlink.FetchFolderNodesResult

/**
 * Use case implementation for fetching folder nodes
 */
fun interface FetchFolderNodes {
    /**
     * Invoke
     *
     * @return Folder nodes result
     */
    suspend operator fun invoke(): FetchFolderNodesResult
}