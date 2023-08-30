package mega.privacy.android.domain.usecase.search

import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.repository.SearchRepository
import javax.inject.Inject

/**
 * Get Search Categories
 *
 * returns list of search categories available for filtering search
 * Currently the values are hardcoded from repository in future we will be taking those from API
 */
class GetSearchCategoriesUseCase @Inject constructor(
    private val searchRepository: SearchRepository,
) {

    /**
     * Invoke
     *
     * @return [SearchCategory]
     */
    operator fun invoke(): List<SearchCategory> = searchRepository.getSearchCategories()

}