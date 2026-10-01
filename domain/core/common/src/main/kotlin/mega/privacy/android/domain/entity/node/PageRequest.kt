package mega.privacy.android.domain.entity.node

/**
 * One page of a node query.
 *
 * @property offset number of rows to skip, in the query's sort order
 * @property limit maximum number of rows to return
 */
data class PageRequest(
    val offset: Int,
    val limit: Int,
) {
    init {
        require(offset >= 0) { "offset must not be negative, was $offset" }
        require(limit > 0) { "limit must be positive, was $limit" }
    }
}
