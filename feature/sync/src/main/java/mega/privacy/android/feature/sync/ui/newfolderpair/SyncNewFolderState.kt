package mega.privacy.android.feature.sync.ui.newfolderpair

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder

/**
 * State of SyncNewFolderScreen
 * @param syncType type of the sync folder pair
 * @param deviceName name of the device
 * @param folderPairName name of the folder pair
 * @param selectedLocalFolder selected local folder
 * @param selectedMegaFolder selected MEGA folder
 * @param showDisableBatteryOptimizationsBanner True if have to show the disable battery optimizations banner, False otherwise
 * @param showAllFilesAccessBanner True if have to show the all files access banner, False otherwise
 * @param showStorageOverQuota True if have to show the storage over quota message, False otherwise
 * @param openSyncListScreen Event to open sync list screen
 * @param showSnackbar Event to show a snackbar message
 */
internal data class SyncNewFolderState(
    val syncType: SyncType,
    val deviceName: String,
    val folderPairName: String = "",
    val selectedLocalFolder: String = "",
    val selectedMegaFolder: RemoteFolder? = null,
    val showDisableBatteryOptimizationsBanner: Boolean = false,
    val showAllFilesAccessBanner: Boolean = false,
    val showStorageOverQuota: Boolean = false,
    val openSyncListScreen: StateEvent = consumed,
    val showSnackbar: StateEventWithContent<Int?> = consumed(),
)