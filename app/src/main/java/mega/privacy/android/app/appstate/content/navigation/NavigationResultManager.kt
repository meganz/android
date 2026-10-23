package mega.privacy.android.app.appstate.content.navigation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import mega.privacy.android.navigation.contract.NavigationResultsHandler
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages navigation results across different navigation handlers.
 * Provides a centralized way to store, monitor, and clear navigation results.
 */
@Singleton
class NavigationResultManager @Inject constructor() : NavigationResultsHandler {
    private val resultFlows = mutableMapOf<String, MutableStateFlow<Any?>>()

    override fun <T> returnResult(key: String, value: T) {
        val resultFlow = resultFlows.getOrPut(key) { MutableStateFlow(null) }
        resultFlow.value = value
    }

    /**
     * Monitors result changes for the given key.
     *
     * @param key The key to monitor
     * @return A Flow that emits the result value when it changes
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T> monitorResult(key: String): Flow<T?> {
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
     * Claims the result for a given key: returns it and clears it in one step.
     *
     * The read and the clear are a single atomic update, so when several screens are composed at
     * once only the first caller receives the value.
     *
     * @param key The key to claim the result for
     * @return the stored result, or null when nothing is stored or another caller claimed it
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T> takeResult(key: String): T? =
        resultFlows[key]?.getAndUpdate { null } as T?

    /**
     * Clears all stored results. Useful for cleanup or when starting fresh.
     */
    override fun clearAllResults() {
        resultFlows.values.forEach { it.value = null }
        resultFlows.clear()
    }
}

