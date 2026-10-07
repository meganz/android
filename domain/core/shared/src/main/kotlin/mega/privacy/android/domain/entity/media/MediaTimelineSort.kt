package mega.privacy.android.domain.entity.media

import mega.privacy.android.domain.entity.node.SortDirection

/**
 * The persisted media timeline sort selection.
 *
 * @property field the timestamp to order by
 * @property direction [SortDirection.Descending] is newest-first, [SortDirection.Ascending] oldest-first
 */
data class MediaTimelineSort(
    val field: MediaTimelineSortField,
    val direction: SortDirection,
) {
    companion object {
        /**
         * Date added, newest first — the ordering used when no sort has been persisted.
         */
        val Default = MediaTimelineSort(
            field = MediaTimelineSortField.DateAdded,
            direction = SortDirection.Descending,
        )
    }
}
