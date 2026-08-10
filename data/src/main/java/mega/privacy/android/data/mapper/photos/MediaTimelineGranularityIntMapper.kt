package mega.privacy.android.data.mapper.photos

import mega.privacy.android.domain.entity.media.MediaTimelineFilter.Granularity
import nz.mega.sdk.MegaGroupNodesByDateFilter
import javax.inject.Inject

/**
 * Two-way mapper between [Granularity] and the matching
 * [MegaGroupNodesByDateFilter] section granularity Int value.
 */
internal class MediaTimelineGranularityIntMapper @Inject constructor() {

    /**
     * Maps a [Granularity] into the SDK section granularity Int value.
     */
    operator fun invoke(granularity: Granularity): Int = when (granularity) {
        Granularity.Day -> MegaGroupNodesByDateFilter.SECTION_GRANULARITY_DAY
        Granularity.Month -> MegaGroupNodesByDateFilter.SECTION_GRANULARITY_MONTH
        Granularity.Year -> MegaGroupNodesByDateFilter.SECTION_GRANULARITY_YEAR
    }

    /**
     * Maps an SDK section granularity Int value back into a [Granularity].
     */
    operator fun invoke(value: Int): Granularity = when (value) {
        MegaGroupNodesByDateFilter.SECTION_GRANULARITY_DAY -> Granularity.Day
        MegaGroupNodesByDateFilter.SECTION_GRANULARITY_MONTH -> Granularity.Month
        MegaGroupNodesByDateFilter.SECTION_GRANULARITY_YEAR -> Granularity.Year
        else -> throw IllegalArgumentException("Unknown section granularity value: $value")
    }
}
