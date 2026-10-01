package mega.privacy.android.domain.entity.home

/**
 * Sort fields available for the pinned home items View-all list.
 */
enum class PinnedHomeItemsSortField {
    /** User-defined order; backed by insertion order until AND-24149 adds position-based ordering. */
    Custom,

    /** Sort by node name. */
    Name,

    /** Sort by the time the item was pinned. */
    DateAdded,
}
