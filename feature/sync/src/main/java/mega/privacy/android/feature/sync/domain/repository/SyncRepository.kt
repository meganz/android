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
    suspend fun setupFolderPair(name: String?, localPath: String, remoteFolderId: Long): Boolean

    /**
     * Returns all setup folder pairs.
     */
    suspend fun getFolderPairs(): List<FolderPair>

    suspend fun removeFolderPair(folderPairId: Long)

    suspend fun pauseSync(folderPairId: Long)

    suspend fun resumeSync(folderPairId: Long)

    fun monitorSyncChanges(): Flow<Unit>
}