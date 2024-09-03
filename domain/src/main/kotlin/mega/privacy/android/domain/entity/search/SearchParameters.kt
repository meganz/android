package mega.privacy.android.domain.entity.search

/**
 * Class representing possible search parameters used for searching via MegaSearchFilterMapper
 *
 * @property query user input
 * @property searchTarget place to search
 * @property searchCategory search filter for file types
 * @property modificationDate last modified date
 * @property creationDate added date
 * @property description description
 * @property tag tag
 */
data class SearchParameters(
    val query: String,
    val searchTarget: SearchTarget = SearchTarget.ROOT_NODES,
    val searchCategory: SearchCategory = SearchCategory.ALL,
    val modificationDate: DateFilterOption? = null,
    val creationDate: DateFilterOption? = null,
    val description: String? = null,
    val tag: String? = null,
)
