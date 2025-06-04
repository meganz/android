package mega.privacy.android.feature.sync.ui.newfolderpair

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder

/**
 * State of SyncNewFolderScreen
 * @property syncType type of the sync folder pair
 * @property deviceName name of the device
 * @property folderPairName name of the folder pair
 * @property selectedLocalFolder selected local folder
 * @property selectedFolderName name of the selected folder
 * @property selectedMegaFolder selected MEGA folder
 * @property showDisableBatteryOptimizationsBanner True if have to show the disable battery optimizations banner, False otherwise
 * @property showAllFilesAccessBanner True if have to show the all files access banner, False otherwise
 * @property showStorageOverQuota True if have to show the storage over quota message, False otherwise
 * @property openSyncListScreen Event to open sync list screen
 * @property showSnackbar Event to show a snackbar message
 */
internal data class SyncNewFolderState(
    val syncType: SyncType,
    val deviceName: String = "",
    val folderPairName: String = "",
    val selectedLocalFolder: String = "",
    val selectedFolderName: String = "",
    val selectedMegaFolder: RemoteFolder? = null,
    val showDisableBatteryOptimizationsBanner: Boolean = false,
    val showAllFilesAccessBanner: Boolean = false,
    val showStorageOverQuota: Boolean = false,
    val openSyncListScreen: StateEvent = consumed,
    val showSnackbar: StateEventWithContent<Int?> = consumed(),
    val showRenameAndCreateBackupDialog: String? = null,
)
