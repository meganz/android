package mega.privacy.android.domain.entity.node

/**
 * Represents the sort order direction.
 */
enum class SortDirection {
    /** Sort in ascending order (A-Z, 1-9, oldest to newest) */
    Ascending,

    /** Sort in descending order (Z-A, 9-1, newest to oldest) */
    Descending
}