package mega.privacy.android.app.appstate.content.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mega.privacy.android.navigation.contract.NavigationHandler

/**
 * Default implementation of NavigationHandler that wraps back stack functionality.
 */
class NavigationHandlerImpl(
    private val backStack: NavBackStack<NavKey>,
) : NavigationHandler {
    private val resultFlows = mutableMapOf<String, MutableStateFlow<Any?>>()

    override fun back() {
        backStack.removeLastOrNull()
    }

    override fun navigate(destination: NavKey) {
        backStack.add(destination)
    }

    override fun navigate(destinations: List<NavKey>) {
        backStack.addAll(destinations)
    }

    override fun backTo(destination: NavKey, inclusive: Boolean) {
        removeFromBackStackTo(destination, inclusive)
    }

    override fun navigateAndClearBackStack(destination: NavKey) {
        backStack.clear()
        backStack.add(destination)
    }

    override fun navigateAndClearTo(destination: NavKey, newParent: NavKey, inclusive: Boolean) {
        removeFromBackStackTo(newParent, inclusive)
        backStack.add(destination)
    }

    override fun <T> returnResult(key: String, value: T) {
        // Store the result in the appropriate StateFlow
        val resultFlow = resultFlows.getOrPut(key) { MutableStateFlow(null) }
        resultFlow.value = value

        // Navigate back after setting the result
        backStack.removeLastOrNull()
    }

    override fun <T> monitorResult(key: String): Flow<T?> {
        // Get or create the StateFlow for this key
        val resultFlow = resultFlows.getOrPut(key) { MutableStateFlow(null) }
        return resultFlow.asStateFlow() as StateFlow<T?>
    }

    /**
     * Clears the result for a specific key after it has been consumed.
     * This helps prevent memory leaks and ensures results are only consumed once.
     *
     * @param key The key to clear the result for
     */
    override fun clearResult(key: String) {
        resultFlows[key]?.value = null
    }

    /**
     * Clears all stored results. Useful for cleanup or when starting fresh.
     */
    fun clearAllResults() {
        resultFlows.values.forEach { it.value = null }
        resultFlows.clear()
    }

    /**
     * Removes elements from the back stack up to the specified destination.
     *
     * @param destination The destination to navigate back to
     * @param inclusive Whether to include the destination in the removal operation
     */
    private fun removeFromBackStackTo(destination: NavKey, inclusive: Boolean) {
        val index = backStack.indexOfLast { it == destination }
        if (index == -1) return
        val removeCount = backStack.size - index - if (inclusive) 0 else 1
        if (removeCount <= 0) return

        repeat(removeCount) {
            backStack.removeLastOrNull()
        }
    }
}