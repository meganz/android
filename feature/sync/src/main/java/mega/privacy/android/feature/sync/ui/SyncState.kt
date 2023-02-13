package mega.privacy.android.feature.sync.ui

import mega.privacy.android.feature.sync.domain.entity.RemoteFolder

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
