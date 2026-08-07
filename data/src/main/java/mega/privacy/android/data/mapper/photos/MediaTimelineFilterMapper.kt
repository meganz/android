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
 * @param subCategoryIntMapper [SubCategoryIntMapper]
 * @param mediaTimelineFavouritesIntMapper [MediaTimelineFavouritesIntMapper]
 * @param megaHandleListMapper [MegaHandleListMapper]
 */
internal class MediaTimelineFilterMapper @Inject constructor(
    private val mediaTimelineGranularityIntMapper: MediaTimelineGranularityIntMapper,
    private val mediaTimelineCategoryIntMapper: MediaTimelineCategoryIntMapper,
    private val mediaTimelineSensitivityIntMapper: MediaTimelineSensitivityIntMapper,
    private val mediaTimelineLocationIntMapper: MediaTimelineLocationIntMapper,
    private val subCategoryIntMapper: SubCategoryIntMapper,
    private val mediaTimelineFavouritesIntMapper: MediaTimelineFavouritesIntMapper,
    private val megaHandleListMapper: MegaHandleListMapper,
) {

    /**
     * @param filter the domain filter describing how to scope and group media nodes.
     * @param timezoneOffset the device's current UTC offset formatted as ±HH:MM (e.g. "+09:00").
     */
    operator fun invoke(
        filter: MediaTimelineFilter,
        timezoneOffset: String,
    ): MegaGroupNodesByDateFilter =
        MegaGroupNodesByDateFilter.createInstance().also {
            val hasSubCategory = filter.subCategory != MediaTimelineFilter.SubCategory.All
            // A sub-category spans all visual media; the concrete category is narrowed by the sub-category.
            val category =
                if (hasSubCategory) MediaTimelineFilter.Category.All else filter.category
            it.byGranularity(mediaTimelineGranularityIntMapper(filter.granularity))
            it.byCategory(mediaTimelineCategoryIntMapper(category))
            it.bySensitivity(mediaTimelineSensitivityIntMapper(filter.sensitivity))
            it.applyMediaTimelineLocation(
                filter = filter,
                locationIntMapper = mediaTimelineLocationIntMapper,
                megaHandleListMapper = megaHandleListMapper,
            )
            it.byUtcOffset(timezoneOffset)
            it.bySubCategory(subCategoryIntMapper(filter.subCategory))
            it.byFavourite(mediaTimelineFavouritesIntMapper(filter.favourites))
        }
}
