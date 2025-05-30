package mega.privacy.android.feature.sync.data.gateway

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.feature.sync.data.model.MegaSyncListenerEvent
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaSync
import nz.mega.sdk.MegaSync.SyncType
import nz.mega.sdk.MegaSyncList
import nz.mega.sdk.MegaSyncStall
import nz.mega.sdk.MegaSyncStats

/**
 * Gateway for accessing Sync portion of Mega API
 */
internal interface SyncGateway {

    /**
     * Flow that emits events when syncs are created, deleted, paused, resumed, etc.
     */
    val syncUpdate: Flow<MegaSyncListenerEvent>

    /**
     * Creates a new folder pair between localPath and MEGA folder
     *
     * @param syncType Sync type of the folder pair
     * @param name Name of the folder pair
     * @param localPath Local path on the device
     * @param remoteFolderId MEGA folder handle
     * @return The backup ID in case of folder pair was set up successfully or null otherwise
     */
    suspend fun syncFolderPair(
        syncType: SyncType,
        name: String?,
        localPath: String,
        remoteFolderId: Long
    ): Long?

    /**
     * Returns all folder pairs
     */
    suspend fun getFolderPairs(): MegaSyncList

    /**
     * Remove folder pair (a sync)
     *
     * @param folderPairId
     */
    suspend fun removeFolderPair(folderPairId: Long)

    /**
     * Monitor changes to MegaSync objects
     *
     */
    fun monitorOnSyncDeleted(): Flow<MegaSync>

    fun monitorOnSyncStatsUpdated(): Flow<MegaSyncStats>

    fun monitorOnSyncStateChanged(): Flow<MegaSync>

    /**
     * Resume sync
     *
     * @param folderPairId - id of the folder pair to resume
     */
    fun resumeSync(folderPairId: Long)

    /**
     * Pause sync
     *
     * @param folderPairId - id of the folder pair to pause
     */
    fun pauseSync(folderPairId: Long)

    /**
     * Get sync stalled issues
     */
    suspend fun getSyncStalledIssues(): List<MegaSyncStall>?

    suspend fun isNodeSyncableWithError(megaNode: MegaNode): MegaError

    /**
     * Change the local path that is being used as root for a sync.
     *
     * @param syncBackupId - id of the folder pair to change
     * @param newLocalSyncRootUri - new local uri to be used as root for the sync
     */
    suspend fun changeSyncLocalRoot(
        syncBackupId: Long,
        newLocalSyncRootUri: String,
    ): Long?
}
