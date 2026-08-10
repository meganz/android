package mega.privacy.android.domain.entity.media

/**
 * A single date-grouped section of the media timeline, as produced for a given [MediaTimelineFilter].
 *
 * @property groupId Identifier of the date group this section represents.
 * @property startDate Start of the date range covered by this section, as a timestamp.
 * @property endDate End of the date range covered by this section, as a timestamp.
 * @property count Number of media nodes contained in this section.
 */
data class MediaTimelineSection(
    val groupId: String,
    val startDate: Long,
    val endDate: Long,
    val count: Long
)