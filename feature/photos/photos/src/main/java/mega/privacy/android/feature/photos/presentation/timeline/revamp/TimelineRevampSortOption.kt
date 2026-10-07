package mega.privacy.android.feature.photos.presentation.timeline.revamp

import androidx.annotation.StringRes
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.media.MediaTimelineSort
import mega.privacy.android.domain.entity.media.MediaTimelineSortField
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.entity.photos.Sort
import mega.privacy.android.shared.nodes.model.SortOptionItem
import mega.privacy.android.shared.resources.R as sharedR

internal const val TIMELINE_SORT_OPTION_DATE_TAKEN_TAG = "timeline_sort_option:date_taken"
internal const val TIMELINE_SORT_OPTION_DATE_ADDED_TAG = "timeline_sort_option:date_added"

/**
 * The timestamp the Timeline Revamp orders and buckets its media by, shown as the rows of the sort
 * bottom sheet.
 */
enum class TimelineRevampSortOption(
    @StringRes override val displayName: Int,
    override val testTag: String,
    override val defaultSortDirection: SortDirection = SortDirection.Descending,
) : SortOptionItem {
    /**
     * The media capture timestamp — when the photo or video was taken. Media without capture
     * metadata carries no such timestamp and is excluded from the timeline.
     */
    DateTaken(
        displayName = sharedR.string.media_timeline_sort_option_date_taken,
        testTag = TIMELINE_SORT_OPTION_DATE_TAKEN_TAG,
    ),

    /**
     * The node modification timestamp, which every node carries.
     */
    DateAdded(
        displayName = sharedR.string.media_timeline_sort_option_date_added,
        testTag = TIMELINE_SORT_OPTION_DATE_ADDED_TAG,
    ),
}

/**
 * The Timeline Revamp's sort selection: which timestamp to order by, and in which direction.
 *
 * @property option the timestamp column
 * @property direction [SortDirection.Descending] is newest-first, [SortDirection.Ascending] oldest-first
 */
data class TimelineRevampSortConfiguration(
    val option: TimelineRevampSortOption,
    val direction: SortDirection,
) {
    /**
     * The SDK ordering to query the timeline sections and pages with.
     *
     * `ORDER_MEDIATS_*` is only accepted for a media category, which every filter the timeline
     * builds satisfies.
     */
    val sortOrder: SortOrder
        get() = when (option) {
            TimelineRevampSortOption.DateTaken -> if (direction == SortDirection.Ascending) {
                SortOrder.ORDER_MEDIATS_ASC
            } else {
                SortOrder.ORDER_MEDIATS_DESC
            }

            TimelineRevampSortOption.DateAdded -> if (direction == SortDirection.Ascending) {
                SortOrder.ORDER_MODIFICATION_ASC
            } else {
                SortOrder.ORDER_MODIFICATION_DESC
            }
        }

    companion object {
        /**
         * The selection for [MediaTimelineSort.Default].
         */
        val Default = MediaTimelineSort.Default.toTimelineRevampSortConfiguration()

        /**
         * The direction as the legacy [Sort], for the screens that still speak newest / oldest only.
         * The timestamp column is carried separately by [sortOrder].
         */
        fun TimelineRevampSortConfiguration.toLegacySort(): Sort =
            if (direction == SortDirection.Ascending) Sort.OLDEST else Sort.NEWEST

        /**
         * The selection as the persisted [MediaTimelineSort].
         */
        fun TimelineRevampSortConfiguration.toMediaTimelineSort(): MediaTimelineSort =
            MediaTimelineSort(
                field = when (option) {
                    TimelineRevampSortOption.DateTaken -> MediaTimelineSortField.DateTaken
                    TimelineRevampSortOption.DateAdded -> MediaTimelineSortField.DateAdded
                },
                direction = direction,
            )

        /**
         * The selection restored from a persisted [MediaTimelineSort].
         */
        fun MediaTimelineSort.toTimelineRevampSortConfiguration(): TimelineRevampSortConfiguration =
            TimelineRevampSortConfiguration(
                option = when (field) {
                    MediaTimelineSortField.DateTaken -> TimelineRevampSortOption.DateTaken
                    MediaTimelineSortField.DateAdded -> TimelineRevampSortOption.DateAdded
                },
                direction = direction,
            )
    }
}
