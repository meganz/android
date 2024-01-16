package mega.privacy.android.data.mapper.search

import mega.privacy.android.domain.entity.search.SearchCategory
import nz.mega.sdk.MegaApiAndroid
import javax.inject.Inject

/**
 * Search filter mapper to map [SearchCategory] to Int
 */
class SearchCategoryIntMapper @Inject constructor() {
    /**
     *  Invoke
     *
     *  @param searchCategory [SearchCategory]
     *  @return [Int]
     */
    operator fun invoke(searchCategory: SearchCategory): Int = when (searchCategory) {
        SearchCategory.AUDIO -> MegaApiAndroid.FILE_TYPE_AUDIO
        SearchCategory.VIDEO -> MegaApiAndroid.FILE_TYPE_VIDEO
        SearchCategory.ALL_DOCUMENTS -> MegaApiAndroid.FILE_TYPE_ALL_DOCS
        SearchCategory.IMAGES -> MegaApiAndroid.FILE_TYPE_PHOTO
        else -> MegaApiAndroid.FILE_TYPE_DEFAULT
    }
}