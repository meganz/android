package mega.privacy.android.feature.photos.presentation.timeline.revamp.mapper

import mega.privacy.android.domain.entity.media.MediaTimelineSection
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * Maps a [MediaTimelineSection] to the calendar date it was bucketed under, for display.
 *
 * Reads [MediaTimelineSection.groupId] rather than [MediaTimelineSection.startDate]: the SDK buckets
 * with a single fixed UTC offset and reports `startDate` as that bucket's local midnight expressed as
 * a UTC instant. Re-deriving the date from `startDate` through the system zone applies the
 * *historical* offset of that instant instead, which lands on the previous day whenever the two
 * offsets differ — every winter section, in every DST zone, viewed during summer. `groupId` is the
 * bucket's local date in the offset the SDK actually grouped by, so it cannot disagree.
 *
 * `startDate` / `endDate` remain the bounds to anchor pagination with; only labels come from here.
 */
class MediaTimelineSectionDateMapper @Inject constructor() {

    /**
     * Accepts every granularity the SDK emits: `2000-01-01` (day), `2000-01` (month) and `2000`
     * (year), the latter two resolving to the first day of the period. Falls back to the system-zone
     * reading of [MediaTimelineSection.startDate] if the id is not in one of those forms.
     */
    operator fun invoke(section: MediaTimelineSection): LocalDate =
        parseGroupId(section.groupId)
            ?: Instant.ofEpochSecond(section.startDate).atZone(ZoneId.systemDefault()).toLocalDate()

    private fun parseGroupId(groupId: String): LocalDate? {
        val parts = groupId.split('-')
        if (parts.size > 3) return null
        val year = parts[0].toIntOrNull() ?: return null
        val month = parts.getOrNull(1)?.let { it.toIntOrNull() ?: return null } ?: 1
        val day = parts.getOrNull(2)?.let { it.toIntOrNull() ?: return null } ?: 1
        return runCatching { LocalDate.of(year, month, day) }.getOrNull()
    }
}
