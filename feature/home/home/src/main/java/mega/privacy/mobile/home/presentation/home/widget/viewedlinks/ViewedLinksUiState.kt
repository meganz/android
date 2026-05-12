package mega.privacy.mobile.home.presentation.home.widget.viewedlinks

import androidx.annotation.DrawableRes
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.node.ViewedLink

/**
 * UI state for the Viewed Links screen and widget.
 *
 * Item loading is driven by `LazyPagingItems.loadState` in the UI layer, so this state
 * only carries one-shot events.
 *
 * @property clearAllLinksEvent Event triggered when the viewed links history has been cleared.
 */
data class ViewedLinksUiState(
    val clearAllLinksEvent: StateEvent = consumed,
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
