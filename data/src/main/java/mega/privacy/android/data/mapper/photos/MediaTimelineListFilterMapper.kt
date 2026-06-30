package mega.privacy.android.data.mapper.photos

import mega.privacy.android.data.mapper.handles.MegaHandleListMapper
import mega.privacy.android.domain.entity.media.MediaTimelineFilter
import nz.mega.sdk.MegaListAllNodesFilter
import javax.inject.Inject

/**
 * Mapper to convert a [MediaTimelineFilter] into the SDK [MegaListAllNodesFilter] used by
 * `listAllNodesByPage`. Granularity is not applicable to this filter (it has no date buckets),
 * so only category, sensitivity and location are mapped.
 *
 * The category/sensitivity/location Int mappers return SDK constants whose values are identical
 * across the SDK filter classes, so they are reused here.
 *
 * @param mediaTimelineCategoryIntMapper [MediaTimelineCategoryIntMapper]
 * @param mediaTimelineSensitivityIntMapper [MediaTimelineSensitivityIntMapper]
 * @param mediaTimelineLocationIntMapper [MediaTimelineLocationIntMapper]
 * @param megaHandleListMapper [MegaHandleListMapper]
 */
internal class MediaTimelineListFilterMapper @Inject constructor(
    private val mediaTimelineCategoryIntMapper: MediaTimelineCategoryIntMapper,
    private val mediaTimelineSensitivityIntMapper: MediaTimelineSensitivityIntMapper,
    private val mediaTimelineLocationIntMapper: MediaTimelineLocationIntMapper,
    private val megaHandleListMapper: MegaHandleListMapper,
) {

    operator fun invoke(filter: MediaTimelineFilter): MegaListAllNodesFilter =
        MegaListAllNodesFilter.createInstance().also {
            it.byCategory(mediaTimelineCategoryIntMapper(filter.category))
            it.bySensitivity(mediaTimelineSensitivityIntMapper(filter.sensitivity))
            it.applyMediaTimelineLocation(
                filter = filter,
                locationIntMapper = mediaTimelineLocationIntMapper,
                megaHandleListMapper = megaHandleListMapper,
            )
        }
}
