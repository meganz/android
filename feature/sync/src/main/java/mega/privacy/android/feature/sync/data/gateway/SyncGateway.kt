package mega.privacy.android.feature.sync.data.gateway

import kotlinx.coroutines.flow.Flow
import nz.mega.sdk.MegaSync
import nz.mega.sdk.MegaSyncList

/**
 * Gateway for accessing Sync portion of Mega API
 */
internal interface SyncGateway {

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

    /**
     * Removes all folder pairs
     *
     */
    @Deprecated("Use removeFolderPair(..) instead to remove a single folder pair")
    suspend fun removeFolderPairs()

    suspend fun removeFolderPair(folderPairId: Long)

    /**
     * Monitor changes to MegaSync objects
     *
     */
    fun monitorSync(): Flow<MegaSync>

    /**
     * Resume all syncs
     *
     */
    fun resumeAllSyncs()

    /**
     * Pause all syncs
     *
     */
    fun pauseAllSyncs()
}

