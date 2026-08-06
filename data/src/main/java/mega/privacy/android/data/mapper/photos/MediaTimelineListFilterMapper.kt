package mega.privacy.android.data.mapper.photos

import mega.privacy.android.data.mapper.handles.MegaHandleListMapper
import mega.privacy.android.domain.entity.media.MediaTimelineFilter
import nz.mega.sdk.MegaListAllNodesFilter
import javax.inject.Inject

/**
 * Mapper to convert a [MediaTimelineFilter] into the SDK [MegaListAllNodesFilter] used by
 * `listAllNodesByPage`. Granularity is not applicable to this filter (it has no date buckets),
 * so only category, sub-category, favourites, sensitivity and location are mapped.
 *
 * The category/sensitivity/location Int mappers return SDK constants whose values are identical
 * across the SDK filter classes, so they are reused here.
 *
 * @param mediaTimelineCategoryIntMapper [MediaTimelineCategoryIntMapper]
 * @param mediaTimelineSensitivityIntMapper [MediaTimelineSensitivityIntMapper]
 * @param mediaTimelineLocationIntMapper [MediaTimelineLocationIntMapper]
 * @param subCategoryIntMapper [SubCategoryIntMapper]
 * @param mediaTimelineFavouritesIntMapper [MediaTimelineFavouritesIntMapper]
 * @param megaHandleListMapper [MegaHandleListMapper]
 */
internal class MediaTimelineListFilterMapper @Inject constructor(
    private val mediaTimelineCategoryIntMapper: MediaTimelineCategoryIntMapper,
    private val mediaTimelineSensitivityIntMapper: MediaTimelineSensitivityIntMapper,
    private val mediaTimelineLocationIntMapper: MediaTimelineLocationIntMapper,
    private val subCategoryIntMapper: SubCategoryIntMapper,
    private val mediaTimelineFavouritesIntMapper: MediaTimelineFavouritesIntMapper,
    private val megaHandleListMapper: MegaHandleListMapper,
) {

    operator fun invoke(filter: MediaTimelineFilter): MegaListAllNodesFilter =
        MegaListAllNodesFilter.createInstance().also {
            val hasSubCategory = filter.subCategory != MediaTimelineFilter.SubCategory.All
            // A sub-category spans all visual media; the concrete category is narrowed by the sub-category.
            val category =
                if (hasSubCategory) MediaTimelineFilter.Category.All else filter.category
            it.byCategory(mediaTimelineCategoryIntMapper(category))
            it.bySensitivity(mediaTimelineSensitivityIntMapper(filter.sensitivity))
            it.applyMediaTimelineLocation(
                filter = filter,
                locationIntMapper = mediaTimelineLocationIntMapper,
                megaHandleListMapper = megaHandleListMapper,
            )
            it.bySubCategory(subCategoryIntMapper(filter.subCategory))
            it.byFavourite(mediaTimelineFavouritesIntMapper(filter.favourites))
        }
}
