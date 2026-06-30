package mega.privacy.android.data.mapper.photos

import mega.privacy.android.data.mapper.handles.MegaHandleListMapper
import mega.privacy.android.domain.entity.media.MediaTimelineFilter
import nz.mega.sdk.MegaGroupNodesByDateFilter
import javax.inject.Inject

/**
 * Mapper to convert a [MediaTimelineFilter] into the SDK [MegaGroupNodesByDateFilter].
 *
 * @param mediaTimelineGranularityIntMapper [MediaTimelineGranularityIntMapper]
 * @param mediaTimelineCategoryIntMapper [MediaTimelineCategoryIntMapper]
 * @param mediaTimelineSensitivityIntMapper [MediaTimelineSensitivityIntMapper]
 * @param mediaTimelineLocationIntMapper [MediaTimelineLocationIntMapper]
 * @param megaHandleListMapper [MegaHandleListMapper]
 */
internal class MediaTimelineFilterMapper @Inject constructor(
    private val mediaTimelineGranularityIntMapper: MediaTimelineGranularityIntMapper,
    private val mediaTimelineCategoryIntMapper: MediaTimelineCategoryIntMapper,
    private val mediaTimelineSensitivityIntMapper: MediaTimelineSensitivityIntMapper,
    private val mediaTimelineLocationIntMapper: MediaTimelineLocationIntMapper,
    private val megaHandleListMapper: MegaHandleListMapper,
) {

    operator fun invoke(filter: MediaTimelineFilter): MegaGroupNodesByDateFilter =
        MegaGroupNodesByDateFilter.createInstance().also {
            it.byGranularity(mediaTimelineGranularityIntMapper(filter.granularity))
            it.byCategory(mediaTimelineCategoryIntMapper(filter.category))
            it.bySensitivity(mediaTimelineSensitivityIntMapper(filter.sensitivity))
            it.applyMediaTimelineLocation(
                filter = filter,
                locationIntMapper = mediaTimelineLocationIntMapper,
                megaHandleListMapper = megaHandleListMapper,
            )
        }
}
