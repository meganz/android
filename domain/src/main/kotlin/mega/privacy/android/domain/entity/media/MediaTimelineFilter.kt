package mega.privacy.android.domain.entity.media

import mega.privacy.android.domain.entity.node.NodeId

/**
 * Describes how media nodes should be scoped and grouped by date when building the media timeline.
 *
 * @property granularity How the timeline sections are grouped by date.
 * @property category Which kind of media nodes to include.
 * @property location Which storage locations to include.
 * @property sensitivity Whether sensitive nodes are included.
 * @property includeLocationHandles When non-empty, restrict the timeline to nodes under these folder
 * handles (e.g. Camera Upload + Media Upload), taking precedence over [location].
 * @property excludeLocationHandles When non-empty, exclude nodes under these folder handles from the
 * [location] scope (e.g. Cloud Drive excluding Camera Upload). Ignored when [includeLocationHandles]
 * is set.
 */
data class MediaTimelineFilter(
    val granularity: Granularity,
    val category: Category,
    val location: Location,
    val sensitivity: Sensitivity,
    val includeLocationHandles: List<NodeId> = emptyList(),
    val excludeLocationHandles: List<NodeId> = emptyList(),
) {
    /**
     * The date granularity used to group media nodes into timeline sections.
     */
    enum class Granularity {
        /**
         * Group nodes by day.
         */
        Day,

        /**
         * Group nodes by month.
         */
        Month,

        /**
         * Group nodes by year.
         */
        Year
    }

    /**
     * The category of media nodes to include in the timeline.
     */
    enum class Category {
        /**
         * Include image nodes only.
         */
        Photos,

        /**
         * Include video nodes only.
         */
        Videos,

        /**
         * Include both image and video nodes.
         */
        All
    }

    /**
     * The storage locations to include when collecting media nodes.
     */
    enum class Location {
        /**
         * Cloud Drive only.
         */
        CloudDrive,

        /**
         * Cloud Drive and Vault (Backups).
         */
        CloudDriveAndVault,

        /**
         * Cloud Drive, Vault (Backups) and Rubbish Bin.
         */
        CloudDriveVaultAndRubbish
    }

    /**
     * Whether sensitive (hidden) nodes are included in the timeline.
     */
    enum class Sensitivity {
        /**
         * Include all nodes, including sensitive ones.
         */
        ShowAll,

        /**
         * Exclude sensitive nodes.
         */
        HideSensitive
    }
}
