package mega.privacy.android.app.presentation.search.model

import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.search.SearchCategory

/**
 * State for SearchActivity
 * @property searchItemList list of search items in [TypedNode]
 * @property searchType search filter it could be anything from [SearchCategory]
 * @property parentHandle Handle of search parent handle where query needed to be searched
 * @property isInProgress to show loading or not
 */
data class SearchActivityState(
    val searchItemList: List<TypedNode> = emptyList(),
    val searchType: SearchCategory = SearchCategory.ALL,
    val parentHandle: Long = -1,
    val isInProgress: Boolean = false
)