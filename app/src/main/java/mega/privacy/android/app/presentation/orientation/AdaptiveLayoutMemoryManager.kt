package mega.privacy.android.app.presentation.orientation

import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adaptive Layout Memory Manager to store and manage adaptive layout state in memory.
 *
 * This manager provides fast access to the adaptive layout configuration without
 * repeatedly calling the feature flag system, improving performance for activities
 * that need to check this value frequently.
 */
@Singleton
class AdaptiveLayoutMemoryManager @Inject constructor() {
    private val adaptiveLayoutEnabled: AtomicReference<Boolean?> = AtomicReference()

    /**
     * Gets the current adaptive layout state.
     *
     * @return true if adaptive layout is enabled, false if disabled, null if not initialized
     */
    fun getAdaptiveLayoutEnabled(): Boolean? = adaptiveLayoutEnabled.get()

    /**
     * Sets the adaptive layout state.
     *
     * @param enabled true if adaptive layout is enabled, false otherwise
     */
    fun setAdaptiveLayoutEnabled(enabled: Boolean) {
        adaptiveLayoutEnabled.set(enabled)
    }

    /**
     * Clears the cached adaptive layout state.
     * Useful for testing or when the state needs to be refreshed.
     */
    fun clearAdaptiveLayoutState() {
        adaptiveLayoutEnabled.set(null)
    }
}
