package mega.privacy.android.app.presentation.offline.offlinev2.model

import mega.privacy.android.domain.entity.offline.OfflineNodeInformation

/**
 * This class is used to show list of offline nodes in the view
 * @property offlineNode [OfflineNodeInformation]
 * @param isSelected offlineNode is selected
 * @param isInvisible offlineNode is invisible
 */
data class OfflineNodeUIItem<T : OfflineNodeInformation>(
    val offlineNode: T,
    var isSelected: Boolean,
    val isInvisible: Boolean,
)