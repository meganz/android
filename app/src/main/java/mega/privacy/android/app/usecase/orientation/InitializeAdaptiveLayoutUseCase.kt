package mega.privacy.android.app.usecase.orientation

import mega.privacy.android.app.presentation.orientation.AdaptiveLayoutMemoryManager
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import javax.inject.Inject

/**
 * Use case to initialize and persist the adaptive layout state in memory.
 *
 * This use case fetches the current adaptive layout configuration and stores it
 * in memory for fast access throughout the application lifecycle.
 */
class InitializeAdaptiveLayoutUseCase @Inject constructor(
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val adaptiveLayoutMemoryManager: AdaptiveLayoutMemoryManager,
) {
    /**
     * Initializes the adaptive layout state by fetching the current value
     * and persisting it in memory.
     *
     * This should be called during app initialization to ensure the value
     * is available for all activities that need it.
     */
    suspend operator fun invoke() {
        val adaptiveLayoutEnabled =
            getFeatureFlagValueUseCase(ApiFeatures.Android16OrientationMigrationEnabled)
        adaptiveLayoutMemoryManager.setAdaptiveLayoutEnabled(adaptiveLayoutEnabled)
    }
}
