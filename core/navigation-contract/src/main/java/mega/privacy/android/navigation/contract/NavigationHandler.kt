package mega.privacy.android.navigation.contract

import androidx.navigation3.runtime.NavKey

/**
 * Interface providing comprehensive navigation capabilities for feature destinations.
 * This replaces the previous function-based navigation approach with a more structured interface.
 */
interface NavigationHandler : NavigationResultsHandler {
    /**
     * Navigate back to the previous screen in the back stack.
     */
    fun back()

    /**
     * Remove a specific destination from the back stack.
     */
    fun remove(navKey: NavKey)

    /**
     * Navigate to a specific destination.
     *
     * Note: Animation options in [NavOptions] (enter/exit transitions) are not supported
     * by this NavigationHandler. Only back stack manipulation options (e.g. popUpTo) are handled.
     *
     * @param destination The destination to navigate to
     * @param navOptions Optional navigation options for customizing navigation behavior
     */
    fun navigate(destination: NavKey, navOptions: NavOptions? = null)

    /**
     * Navigate to a specific destination within a hierarchy
     *
     * Note: Animation options in [NavOptions] (enter/exit transitions) are not supported
     * by this NavigationHandler. Only back stack manipulation options (e.g. popUpTo) are handled.
     *
     * @param destinations
     * @param navOptions Optional navigation options for customizing navigation behavior
     */
    fun navigate(destinations: List<NavKey>, navOptions: NavOptions? = null)

    /**
     * Pop back stack to a specific destination.
     *
     * @param destination The destination to pop back to
     * @param inclusive Whether to include the destination in the pop operation
     */
    fun backTo(destination: NavKey, inclusive: Boolean = false)

    /**
     * Clear the entire back stack and navigate to a destination.
     *
     * @param destination The destination to navigate to
     */
    fun navigateAndClearBackStack(destination: NavKey)

    /**
     * Navigate to a destination and clear the back stack up to a new parent destination.
     *
     * @param destination The destination to navigate to
     * @param newParent The new parent destination to set in the back stack
     * @param inclusive Whether to include the new parent in the pop operation
     */
    fun navigateAndClearTo(destination: NavKey, newParent: NavKey, inclusive: Boolean = false) =
        navigateAndClearTo(listOf(destination), newParent, inclusive)

    /**
     * Navigate to a destination and clear the back stack up to a new parent destination.
     *
     * @param destination The destinations to navigate to
     * @param newParent The new parent destination to set in the back stack
     * @param inclusive Whether to include the new parent in the pop operation
     */
    fun navigateAndClearTo(destination: List<NavKey>, newParent: NavKey, inclusive: Boolean = false)
} 