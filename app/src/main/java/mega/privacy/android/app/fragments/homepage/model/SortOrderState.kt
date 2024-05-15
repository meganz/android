package mega.privacy.android.app.fragments.homepage.model

import mega.privacy.android.domain.entity.SortOrder

/**
 * The state of the sort order.
 *
 * @property cloudSortOrder The sort order for Cloud order.
 * @property othersSortOrder The sort order for Others order (Incoming root)
 * @property offlineSortOrder The sort order for Offline order
 */
data class SortOrderState(
    val cloudSortOrder: SortOrder,
    val othersSortOrder: SortOrder,
    val offlineSortOrder: SortOrder,
)
