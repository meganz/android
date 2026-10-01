package mega.privacy.android.domain.entity.node

/**
 * Domain entity representing a file or folder that was opened via a MEGA deep link.
 *
 * Viewed links are recorded when a user opens a MEGA file or folder link (either from
 * an external deep link or by pasting a URL in the Open Link dialog) and are displayed
 * in the "Viewed Links" screen grouped by date.
 *
 * @property nodeHandle The node handle of the opened file or folder.
 * @property name The display name of the file or folder.
 * @property linkUrl The original MEGA deep link URL that was used to open the node.
 * @property type The type of link, either [RecentlyViewedLinkType.FileLink] or [RecentlyViewedLinkType.FolderLink].
 * @property accessedTimestamp The epoch timestamp (in milliseconds) when the link was last accessed.
 */
data class ViewedLink(
    val nodeHandle: Long,
    val name: String,
    val linkUrl: String,
    val type: RecentlyViewedLinkType,
    val accessedTimestamp: Long? = null,
)
