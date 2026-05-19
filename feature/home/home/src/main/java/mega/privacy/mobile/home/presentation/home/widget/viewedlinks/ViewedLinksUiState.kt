package mega.privacy.mobile.home.presentation.home.widget.viewedlinks

import androidx.annotation.DrawableRes
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.entity.node.ViewedLink
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.NodeSortOption

/**
 * UI state for the Viewed Links screen and widget.
 *
 * @property clearAllLinksEvent Event triggered when the viewed links history has been cleared.
 * @property sortConfiguration The active sort option and direction.
 * @property currentViewType Whether the list is shown as list or grid.
 */
data class ViewedLinksUiState(
    val clearAllLinksEvent: StateEvent = consumed,
    val sortConfiguration: NodeSortConfiguration = NodeSortConfiguration(
        sortOption = NodeSortOption.Created,
        sortDirection = SortDirection.Descending,
    ),
    val currentViewType: ViewType = ViewType.LIST,
)

/**
 * A viewed link with resolved icon and optional preview path for thumbnail display.
 *
 * @property viewedLink The original viewed link data.
 * @property iconRes The drawable resource for the file/folder type icon.
 * @property previewPath The local file path of the preview image, if available.
 */
data class ViewedLinkUiItem(
    val viewedLink: ViewedLink,
    @DrawableRes val iconRes: Int,
    val previewPath: String?,
)
