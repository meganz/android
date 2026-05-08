package mega.privacy.android.domain.entity.continuewhereleftoff

/**
 * Carousel display item for continue-where-you-left-off.
 * Contains only the data needed for the list. Progress detail (playback position,
 * page number, scroll state) is not included here — it is fetched separately via
 * the respective media/PDF repository or
 * [mega.privacy.android.domain.repository.ContinueWhereLeftOffRepository.getTextEditorScroll]
 * when the user selects an item.
 * Thumbnails are resolved in the presentation layer by node handle.
 */
data class ContinueWhereLeftOffItem(
    val nodeHandle: Long,
    val type: RecentlyUsedType,
    val title: String,
    val lastAccessedTimestamp: Long,
    val duration: String? = null,
)
