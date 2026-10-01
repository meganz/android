package mega.privacy.android.domain.entity.continuewhereleftoff

/**
 * Carousel display item for continue-where-you-left-off.
 * Contains only the data needed for the list. Progress detail (playback position,
 * page number, scroll state) is not included here — it is fetched separately via
 * the respective media/PDF repository or
 * [mega.privacy.android.domain.repository.ContinueWhereLeftOffRepository.getTextEditorScroll]
 * when the user selects an item.
 * Thumbnails are resolved in the presentation layer by node handle.
 *
 * @property isSensitive whether the node is hidden (directly marked sensitive or sensitive
 * through an ancestor). True only when the hidden-nodes feature is active for the account and
 * the user is showing hidden items; the carousel uses it to blur the thumbnail, mirroring how
 * the node lists render sensitive items. When hidden items are not being shown they are removed
 * from the list entirely, so a surviving item is never flagged sensitive in that case.
 * @property isTakenDown whether the node has been taken down (e.g. for a copyright violation).
 * Taken-down nodes must not show their original thumbnail; the carousel and list show the
 * generic file-type icon instead, mirroring how the node lists render taken-down content.
 */
data class ContinueWhereLeftOffItem(
    val nodeHandle: Long,
    val type: RecentlyUsedType,
    val title: String,
    val lastAccessedTimestamp: Long,
    val duration: String? = null,
    val isSensitive: Boolean = false,
    val isTakenDown: Boolean = false,
)
