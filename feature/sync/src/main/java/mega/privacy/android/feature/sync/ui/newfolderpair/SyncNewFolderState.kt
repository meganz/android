package mega.privacy.android.feature.sync.ui.newfolderpair

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder

/**
 * State of NewFolderPairScreen
 * @param folderPairName name of the folder pair
 * @param selectedLocalFolder selected local folder
 * @param selectedMegaFolder selected MEGA folder
 */
internal data class SyncNewFolderState(
    val folderPairName: String = "",
    val selectedLocalFolder: String = "",
    val selectedMegaFolder: RemoteFolder? = null,
    val showDisableBatteryOptimizationsBanner: Boolean = false,
    val showAllFilesAccessBanner: Boolean = false,
    val showStorageOverQuota: Boolean = false,
    val openSyncListScreen: StateEvent = consumed,
)