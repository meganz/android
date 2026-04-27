package mega.privacy.mobile.home.presentation.home.widget.viewedlinks

import androidx.annotation.DrawableRes
import mega.privacy.android.domain.entity.node.ViewedLink

/**
 * UI state for the Viewed Links widget on the Home page.
 *
 * @property isLoading Whether the widget is still loading data.
 * @property items The list of viewed link items to display.
 */
data class ViewedLinksUiState(
    val isLoading: Boolean = true,
    val items: List<ViewedLinkUiItem> = emptyList(),
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
