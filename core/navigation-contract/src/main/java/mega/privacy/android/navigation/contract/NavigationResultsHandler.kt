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
     * Claims the result for a given key: returns it and clears it in one step.
     *
     * [monitorResult] delivers a result to every collector, so a result that must drive an action
     * exactly once cannot be read from the flow. Claiming is atomic: only the first caller
     * receives the value and the rest receive null.
     *
     * @param T type of the result
     * @param key the key to claim the result for
     * @return the stored result, or null when nothing is stored or another caller claimed it
     */
    fun <T> takeResult(key: String): T?

    /**
     * Clears all stored results. Useful for cleanup or when starting fresh.
     */
    fun clearAllResults()
}