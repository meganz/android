package mega.privacy.android.feature.sync.ui.newfolderpair

import mega.privacy.android.feature.sync.domain.entity.RemoteFolder

/**
 * State of NewFolderPairScreen
 * @param folderPairName name of the folder pair
 * @param selectedLocalFolder selected local folder
 * @param selectedMegaFolder selected MEGA folder
 */
data class SyncNewFolderState(
    val folderPairName: String = "",
    val selectedLocalFolder: String = "",
    val selectedMegaFolder: RemoteFolder? = null,
)