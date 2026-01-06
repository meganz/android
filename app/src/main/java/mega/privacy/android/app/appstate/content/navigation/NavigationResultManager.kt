package mega.privacy.android.app.appstate.content.navigation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
     * Clears all stored results. Useful for cleanup or when starting fresh.
     */
    override fun clearAllResults() {
        resultFlows.values.forEach { it.value = null }
        resultFlows.clear()
    }
}

