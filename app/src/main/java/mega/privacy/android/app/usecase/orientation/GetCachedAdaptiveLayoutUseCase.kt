package mega.privacy.android.app.usecase.orientation

import mega.privacy.android.app.presentation.orientation.AdaptiveLayoutMemoryManager
import javax.inject.Inject

/**
 * Use case to retrieve the cached adaptive layout state from memory.
 *
 * This use case provides fast access to the adaptive layout configuration
 * without making expensive feature flag calls. If the value hasn't been
 * initialized, it returns false as a safe default.
 */
class GetCachedAdaptiveLayoutUseCase @Inject constructor(
    private val adaptiveLayoutMemoryManager: AdaptiveLayoutMemoryManager,
) {
    /**
     * Gets the cached adaptive layout state.
     *
     * @return true if adaptive layout is enabled, false if disabled or not initialized
     */
    operator fun invoke(): Boolean = adaptiveLayoutMemoryManager.getAdaptiveLayoutEnabled() ?: false

}
