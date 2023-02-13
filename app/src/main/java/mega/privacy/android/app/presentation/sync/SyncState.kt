package mega.privacy.android.app.presentation.sync

import mega.privacy.android.domain.entity.sync.RemoteFolder

/**
 * @param selectedLocalFolder selected local folder
 * @param selectedMegaFolder selected MEGA folder
 * @param rootMegaRemoteFolders root MEGA remote folders
 * @param isSyncing is syncing in progress
 */
data class SyncState(
    val selectedLocalFolder: String = "",
    val selectedMegaFolder: RemoteFolder? = null,
    val rootMegaRemoteFolders: List<RemoteFolder> = emptyList(),
    val isSyncing: Boolean = false,
)
