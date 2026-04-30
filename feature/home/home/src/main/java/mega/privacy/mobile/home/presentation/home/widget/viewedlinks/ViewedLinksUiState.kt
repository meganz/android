package mega.privacy.mobile.home.presentation.home.widget.viewedlinks

import androidx.annotation.DrawableRes
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.node.ViewedLink

/**
 * UI state for the Viewed Links widget on the Home page.
 */
sealed interface ViewedLinksUiState {

    /**
     * Initial state while viewed links are being loaded.
     */
    data object Loading : ViewedLinksUiState

    /**
     * Loaded state containing the resolved viewed link items.
     *
     * @property items The list of viewed link items to display.
     * @property clearAllLinksEvent Event triggered when the viewed links history has been cleared.
     */
    data class Ready(
        val items: List<ViewedLinkUiItem>,
        val clearAllLinksEvent: StateEvent = consumed,
    ) : ViewedLinksUiState
}

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
