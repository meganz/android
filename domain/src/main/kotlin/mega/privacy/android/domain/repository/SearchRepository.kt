package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.search.SearchCategory

/**
 * Search Repository
 *
 * All the methods related to search will be included here
 */
interface SearchRepository {

    /**
     * Get all search filters
     * returns list of search categories available for filtering search
     * Currently the values are hardcoded from repository in future we will be taking those from API
     */
    fun getSearchCategories(): List<SearchCategory>
}