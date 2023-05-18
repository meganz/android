package mega.privacy.android.feature.sync.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.feature.sync.domain.entity.FolderPair

/**
 * Repository for syncing folder pairs
 *
 */
interface SyncRepository {

    /**
     * Establishes a pair between local and remote directories and starts the syncing process
     */
    suspend fun setupFolderPair(localPath: String, remoteFolderId: Long): Boolean

    /**
     * Returns all setup folder pairs.
     */
    suspend fun getFolderPairs(): List<FolderPair>

    /**
     * Removes all folder pairs
     */
    suspend fun removeFolderPairs()

    suspend fun resumeAllSyncs()

    suspend fun pauseAllSyncs()

    fun monitorSync(): Flow<FolderPair>
}