package mega.privacy.android.feature.clouddrive.presentation.offline.model

import mega.privacy.android.domain.entity.offline.OfflineFileInformation

/**
 * This class is used to show list of offline nodes in the view
 * @property offlineFileInformation [OfflineFileInformation]
 * @param isSelected offlineNode is selected
 * @param isInvisible
 * @param isHighlighted offlineNode is highlighted because it comes from "Locate" action in notification
 */
data class OfflineNodeUiItem(
    val offlineFileInformation: OfflineFileInformation,
    val isSelected: Boolean = false,
    val isInvisible: Boolean = false,
    val isHighlighted: Boolean = false,
)
