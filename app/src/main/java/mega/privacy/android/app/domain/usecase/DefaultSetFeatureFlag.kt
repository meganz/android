package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.FeatureFlagRepository
import javax.inject.Inject

/**
 * Implementation of @SetFeatureFlag use case
 *
 * @param repository: @FeatureFlagRepository where the feature flag value will be updated
 */
class DefaultSetFeatureFlag @Inject constructor(private val repository: FeatureFlagRepository) :
    SetFeatureFlag {

    /**
     * Invoke.
     * @param featureName: Feature Name
     * @param isEnabled: Boolena value
     */
    override suspend fun invoke(featureName: String, isEnabled: Boolean) {
        repository.setFeature(featureName, isEnabled)
    }
}