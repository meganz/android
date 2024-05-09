package mega.privacy.android.app.presentation.offline.offlinecompose.model

import mega.privacy.android.app.presentation.offline.offlinefileinfocompose.model.OfflineFileInfoUiState
import mega.privacy.android.domain.entity.offline.OfflineNodeInformation

/**
 * This class is used to show list of offline nodes in the view
 * @property offlineNode [OfflineNodeInformation]
 * @param isSelected offlineNode is selected
 * @param isInvisible
 */
data class OfflineNodeUIItem(
    val offlineNode: OfflineFileInfoUiState,
    var isSelected: Boolean = false,
    var isInvisible: Boolean = false
)