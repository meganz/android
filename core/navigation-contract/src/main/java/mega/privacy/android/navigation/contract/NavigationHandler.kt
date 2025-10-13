package mega.privacy.android.navigation.contract

import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.Flow

/**
 * Interface providing comprehensive navigation capabilities for feature destinations.
 * This replaces the previous function-based navigation approach with a more structured interface.
 */
interface NavigationHandler {
    /**
     * Navigate back to the previous screen in the back stack.
     */
    fun back()

    /**
     * Navigate to a specific destination.
     *
     * @param destination The destination to navigate to
     */
    fun navigate(destination: NavKey)

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
    fun navigateAndClearTo(destination: NavKey, newParent: NavKey, inclusive: Boolean = false)

    /**
     * Set result and pop
     *
     * @param T type of the result
     * @param key type of the result
     * @param value
     */
    fun <T> returnResult(key: String, value: T)

    /**
     * Clear result for a given key
     *
     * @param key
     */
    fun clearResult(key: String)

    /**
     * Monitor result
     *
     * @param T
     * @param key
     * @return flow that emits when value is returned
     */
    fun <T> monitorResult(key: String): Flow<T?>
} 