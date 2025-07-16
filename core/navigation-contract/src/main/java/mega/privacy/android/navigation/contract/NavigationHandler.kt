package mega.privacy.android.navigation.contract

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
    fun navigate(destination: Any)

    /**
     * Pop back stack to a specific destination.
     *
     * @param destination The destination to pop back to
     * @param inclusive Whether to include the destination in the pop operation
     */
    fun backTo(destination: Any, inclusive: Boolean = false)

    /**
     * Clear the entire back stack and navigate to a destination.
     *
     * @param destination The destination to navigate to
     */
    fun navigateAndClearBackStack(destination: Any)

    /**
     * Set result and pop
     *
     * @param T type of the result
     * @param key type of the result
     * @param value
     */
    fun <T> returnResult(key: String, value: T)

    /**
     * Monitor result
     *
     * @param T
     * @param key
     * @return flow that emits when value is returned
     */
    fun <T> monitorResult(key: String): Flow<T?>
} 