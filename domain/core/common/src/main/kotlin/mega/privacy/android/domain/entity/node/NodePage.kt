package mega.privacy.android.domain.entity.node

/**
 * The result of a [PageRequest].
 *
 * @property nodes the rows of this page, in the query's sort order
 * @property nextOffset the offset of the row after this page. It is the request offset plus the
 * rows the query returned for the page, so it stays correct even if a caller drops rows.
 * @property hasMore true when the query had rows beyond this page
 */
data class NodePage<T : Node>(
    val nodes: List<T>,
    val nextOffset: Int,
    val hasMore: Boolean,
)
