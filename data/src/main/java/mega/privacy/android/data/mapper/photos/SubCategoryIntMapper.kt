package mega.privacy.android.data.mapper.photos

import mega.privacy.android.domain.entity.media.MediaTimelineFilter.SubCategory
import nz.mega.sdk.MegaNodeScopeFilter
import nz.mega.sdk.MegaSearchFilter
import javax.inject.Inject

/**
 * Maps a [SubCategory] into the SDK sub-category Int value used by
 * `MegaNodeScopeFilter.bySubcategory`.
 *
 * - [SubCategory.All] -> 0 (None, no sub-category restriction)
 * - [SubCategory.Gif] -> 1
 * - [SubCategory.Raw] -> 2
 */
internal class SubCategoryIntMapper @Inject constructor() {

    operator fun invoke(subCategory: SubCategory): Int = when (subCategory) {
        SubCategory.All -> MegaNodeScopeFilter.FILE_SUBTYPE_NONE
        SubCategory.Gif -> MegaNodeScopeFilter.FILE_SUBTYPE_GIF
        SubCategory.Raw -> MegaNodeScopeFilter.FILE_SUBTYPE_RAW
    }

    operator fun invoke(type: Int): SubCategory = when (type) {
        MegaNodeScopeFilter.FILE_SUBTYPE_NONE -> SubCategory.All
        MegaNodeScopeFilter.FILE_SUBTYPE_GIF -> SubCategory.Gif
        MegaNodeScopeFilter.FILE_SUBTYPE_RAW -> SubCategory.Raw
        else -> throw IllegalArgumentException("File subtype unknown!")
    }
}
