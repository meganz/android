package mega.privacy.android.app.presentation.search.model

import mega.privacy.android.domain.entity.search.SearchCategory

/**
 * Data class for search filter
 *
 * @param filter enum SearchCategory
 * @param name the filter name which is displayed to the end user
 */
data class SearchFilter(val filter: SearchCategory, val name: String)
