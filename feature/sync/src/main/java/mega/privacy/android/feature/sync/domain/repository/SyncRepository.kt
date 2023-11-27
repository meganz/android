package mega.privacy.android.feature.sync.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.feature.sync.data.model.MegaSyncListenerEvent
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.StalledIssue

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

    /**
     * Returns the folder pair with the given id.
     *
     * @param folderPairId The id of the folder pair to retrieve.
     */
    suspend fun removeFolderPair(folderPairId: Long)

    /**
     * Pauses the syncing process for the given folder pair.
     *
     * @param folderPairId The id of the folder pair to pause.
     */
    suspend fun pauseSync(folderPairId: Long)

    /**
     * Resumes the syncing process for the given folder pair.
     *
     * @param folderPairId The id of the folder pair to resume.
     */
    suspend fun resumeSync(folderPairId: Long)

    /**
     * Monitors the syncing process for the given folder pair.
     *
     * @return [Flow<Unit>]Returns the folder pair with the given id.
     */
    val syncChanges: Flow<MegaSyncListenerEvent>

    /**
     * Gets the list of stalled issues.
     * @return [List<StalledIssue>] Returns the stalled issues.
     */
    suspend fun getSyncStalledIssues(): List<StalledIssue>

    /**
     * Monitors the list of stalled issues.
     * @return [Flow<List<StalledIssue>>] Returns the stalled issues.
     */
    fun monitorStalledIssues(): Flow<List<StalledIssue>>

    /**
     * Monitors the list of folder pairs.
     *
     * @return [Flow<List<FolderPair>>] Returns the folder pairs.
     */
    fun monitorFolderPairChanges(): Flow<List<FolderPair>>

}