package mega.privacy.android.app.presentation.offline.offlinecompose.model

import mega.privacy.android.domain.entity.offline.OfflineFileInformation

/**
 * This class is used to show list of offline nodes in the view
 * @property offlineNode [OfflineFileInformation]
 * @param isSelected offlineNode is selected
 * @param isInvisible
 * @param isHighlighted offlineNode is highlighted because it comes from "Locate" action in notification
 */
data class OfflineNodeUIItem(
    val offlineNode: OfflineFileInformation,
    var isSelected: Boolean = false,
    var isInvisible: Boolean = false,
    val isHighlighted: Boolean = false,
)