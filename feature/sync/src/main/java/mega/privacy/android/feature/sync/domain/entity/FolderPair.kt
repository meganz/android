package mega.privacy.android.feature.sync.domain.entity

import mega.privacy.android.domain.entity.sync.SyncError
import mega.privacy.android.domain.entity.sync.SyncType

/**
 * Entity representing a folder pair
 * @property id - id of the folder pair
 * @property syncType - the sync type of the folder pair
 * @property pairName - name of the pair
 * @property localFolderPath - path to the local folder
 * @property remoteFolder - remote folder location
 * @property syncStatus - the status of the sync
 * @property syncError - the SDK error that caused the sync to fail
 */
data class FolderPair(
    val id: Long,
    val syncType: SyncType,
    val pairName: String,
    val localFolderPath: String,
    val remoteFolder: RemoteFolder,
    val syncStatus: SyncStatus,
    val syncError: SyncError? = null
) {

    companion object {
        /**
         * Reset the folder pair
         */
        fun empty(): FolderPair =
            FolderPair(
                id = -1,
                syncType = SyncType.TYPE_UNKNOWN,
                pairName = "",
                localFolderPath = "",
                remoteFolder = RemoteFolder(-1, ""),
                syncStatus = SyncStatus.SYNCED,
            )
    }
}
