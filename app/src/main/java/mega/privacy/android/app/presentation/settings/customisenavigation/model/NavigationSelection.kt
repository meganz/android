package mega.privacy.android.app.presentation.settings.customisenavigation.model

/**
 * Maximum number of user-selectable navigation bar items, excluding the pinned Menu item.
 */
const val MaxSelectableNavigationItems = 4

/**
 * Minimum number of user-selectable navigation bar items, excluding the pinned Menu item.
 */
const val MinSelectableNavigationItems = 3

/**
 * Reason the pending navigation selection cannot be persisted.
 */
enum class NavigationSelectionError {

    /**
     * Fewer than [MinSelectableNavigationItems] items are selected.
     */
    TooFewItems,

    /**
     * More than [MaxSelectableNavigationItems] items are selected.
     */
    TooManyItems,
}

/**
 * Adds [id] to the end of the pending selection, ignoring it when already selected.
 */
fun List<String>.addNavigationItem(id: String): List<String> =
    if (id in this) this else this + id

/**
 * Removes [id] from the pending selection.
 */
fun List<String>.removeNavigationItem(id: String): List<String> = this - id

/**
 * The [NavigationSelectionError] for the current selection, or null when its size is within the
 * allowed [MinSelectableNavigationItems]..[MaxSelectableNavigationItems] range.
 */
fun List<String>.navigationSelectionError(): NavigationSelectionError? = when {
    size < MinSelectableNavigationItems -> NavigationSelectionError.TooFewItems
    size > MaxSelectableNavigationItems -> NavigationSelectionError.TooManyItems
    else -> null
}

/**
 * Moves the item at [fromIndex] to [toIndex], returning the receiver when either index is
 * out of bounds.
 */
fun List<String>.moveNavigationItem(fromIndex: Int, toIndex: Int): List<String> = when {
    fromIndex == toIndex || fromIndex !in indices || toIndex !in indices -> this
    else -> toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
}
