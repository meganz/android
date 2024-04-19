package mega.privacy.android.feature.sync.ui.newfolderpair

import mega.privacy.android.feature.sync.domain.entity.RemoteFolder

/**
 * State of NewFolderPairScreen
 * @param selectedLocalFolder selected local folder
 * @param selectedMegaFolder selected MEGA folder
 */
internal data class SyncNewFolderState(
    val selectedLocalFolder: String = "",
    val selectedMegaFolder: RemoteFolder? = null,
    val showDisableBatteryOptimizationsBanner: Boolean = false,
    val showAllFilesAccesBanner: Boolean = false
)