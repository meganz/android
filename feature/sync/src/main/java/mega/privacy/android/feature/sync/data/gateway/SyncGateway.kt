package mega.privacy.android.feature.sync.data.gateway

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.feature.sync.data.model.MegaSyncListenerEvent
import nz.mega.sdk.MegaSync
import nz.mega.sdk.MegaSyncList
import nz.mega.sdk.MegaSyncStallList
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
     * @param localPath - local path on the device
     * @param remoteFolderId - MEGA folder handle
     * @return Boolean - indicates whether the folder was set up
     * successfully or not
     */
    suspend fun syncFolderPair(
        name: String?,
        localPath: String,
        remoteFolderId: Long
    ): Boolean

    /**
     * Returns all folder pairs
     */
    suspend fun getFolderPairs(): MegaSyncList

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

    suspend fun getSyncStalledIssues(): MegaSyncStallList?
}

