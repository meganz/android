package mega.privacy.android.data.repository

import mega.privacy.android.data.mapper.search.SearchCategoryMapper
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.repository.SearchRepository
import nz.mega.sdk.MegaApiAndroid
import javax.inject.Inject

/**
 * Search repository impl
 *
 * Implementation of [SearchRepository]
 */
internal class SearchRepositoryImpl @Inject constructor(
    val searchCategoryMapper: SearchCategoryMapper,
) : SearchRepository {
    override fun getSearchCategories(): List<SearchCategory> = listOf(
        MegaApiAndroid.FILE_TYPE_PHOTO,
        MegaApiAndroid.FILE_TYPE_VIDEO,
        MegaApiAndroid.FILE_TYPE_AUDIO,
        MegaApiAndroid.FILE_TYPE_DEFAULT,
        MegaApiAndroid.FILE_TYPE_DOCUMENT,
    ).map {
        searchCategoryMapper(it)
    }
}