package mega.privacy.android.domain.entity.preference

/**
 * Preference describing the user's customised bottom navigation bar
 *
 * @property orderedVisibleItemIds Ordered ids of the items shown in the navigation bar,
 * excluding the pinned Menu item. The first id is the start screen.
 */
data class NavigationItemsPreference(val orderedVisibleItemIds: List<String>)
