package mega.privacy.android.feature.photos.presentation.playlists.videoselect.model

import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.shared.nodes.model.NodeSubtitleText
import mega.privacy.android.domain.entity.node.NodeId

/**
 * UI Entity representing a video item in the select video for playlist screen
 *
 * @property id The unique identifier of the video item
 * @property name The name of the video item
 * @property title The title to be displayed for the video item
 * @property subtitle The subtitle text for the video item
 * @property iconRes The resource ID for the icon representing the video item
 * @property duration The duration of the video item, if applicable
 * @property isSelected Indicates whether the video item is selected
 * @property isSensitive Indicates whether the video item is marked as sensitive
 * @property isTakenDown Indicates whether the video item has been taken down
 * @property isFolder Indicates whether the item is a folder
 * @property isVideo Indicates whether the item is a video
 * @property isSelectable Indicates whether the item is selectable
 */
data class SelectVideoItemUiEntity(
    val id: NodeId,
    val name: String,
    val title: LocalizedText = LocalizedText.Literal(""),
    val subtitle: NodeSubtitleText = NodeSubtitleText.Empty,
    val iconRes: Int = 0,
    val duration: String? = null,
    val isSelected: Boolean = false,
    val isSensitive: Boolean = false,
    val isTakenDown: Boolean = false,
    val isFolder: Boolean = false,
    val isVideo: Boolean = false
) {
    val isSelectable: Boolean get() = !isFolder && isVideo
}
