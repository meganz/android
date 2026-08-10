package mega.privacy.android.navigation.contract

import kotlinx.coroutines.flow.Flow

interface NavigationResultsHandler {

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

    /**
     * Clears all stored results. Useful for cleanup or when starting fresh.
     */
    fun clearAllResults()
}