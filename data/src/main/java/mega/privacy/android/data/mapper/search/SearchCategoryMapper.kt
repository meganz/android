package mega.privacy.android.data.mapper.search

import mega.privacy.android.domain.entity.search.SearchCategory
import nz.mega.sdk.MegaApiAndroid
import javax.inject.Inject

/**
 * Maps MegaApi values to [SearchCategory] enum
 */
internal class SearchCategoryMapper @Inject constructor() {

    /**
     *  Invoke
     *
     *  @param filter
     *  @return [SearchCategory]
     */
    operator fun invoke(filter: Int): SearchCategory = when (filter) {
        MegaApiAndroid.FILE_TYPE_AUDIO -> SearchCategory.AUDIO
        MegaApiAndroid.FILE_TYPE_VIDEO -> SearchCategory.VIDEO
        MegaApiAndroid.FILE_TYPE_ALL_DOCS -> SearchCategory.ALL_DOCUMENTS
        MegaApiAndroid.FILE_TYPE_PHOTO -> SearchCategory.IMAGES
        else -> SearchCategory.ALL
    }
}