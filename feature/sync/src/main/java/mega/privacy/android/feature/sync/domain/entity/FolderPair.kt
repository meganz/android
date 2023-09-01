package mega.privacy.android.feature.sync.domain.entity

/**
 * Entity representing a folder pair
 * @property id - id of the folder pair
 * @property pairName - name of the pair
 * @property localFolderPath - path to the local folder
 * @property remoteFolder - remote folder location
 * @property state - state of the sync of the pair
 */
data class FolderPair(
    val id: Long,
    val pairName: String,
    val localFolderPath: String,
    val remoteFolder: RemoteFolder,
    val syncStatus: SyncStatus
) {

    companion object {
        fun empty(): FolderPair =
            FolderPair(
                id = -1,
                pairName = "",
                localFolderPath = "",
                remoteFolder = RemoteFolder(-1, ""),
                syncStatus = SyncStatus.MONITORING
            )
    }
}
