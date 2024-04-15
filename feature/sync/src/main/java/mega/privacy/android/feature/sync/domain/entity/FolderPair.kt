package mega.privacy.android.feature.sync.domain.entity

import mega.privacy.android.domain.entity.sync.SyncError

/**
 * Entity representing a folder pair
 * @property id - id of the folder pair
 * @property pairName - name of the pair
 * @property localFolderPath - path to the local folder
 * @property remoteFolder - remote folder location
 * @property syncStatus - the status of the sync
 * @property syncError - the SDK error that caused the sync to fail
 */
data class FolderPair(
    val id: Long,
    val pairName: String,
    val localFolderPath: String,
    val remoteFolder: RemoteFolder,
    val syncStatus: SyncStatus,
    val syncError: SyncError? = null
) {

    companion object {
        fun empty(): FolderPair =
            FolderPair(
                id = -1,
                pairName = "",
                localFolderPath = "",
                remoteFolder = RemoteFolder(-1, ""),
                syncStatus = SyncStatus.SYNCED
            )
    }
}
