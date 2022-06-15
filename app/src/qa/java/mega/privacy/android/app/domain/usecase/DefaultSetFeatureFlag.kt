package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.FeatureFlagRepository
import javax.inject.Inject

/**
 * Implementation of @SetFeatureFlag
 * @param repository: Feature flag repository
 */
class DefaultSetFeatureFlag @Inject constructor(private val repository: FeatureFlagRepository) :
    SetFeatureFlag {

    /**
     * Sets value of feature flag
     *
     * @param featureName: Name of the feature
     * @param isEnabled: Boolean value
     */
    override suspend fun invoke(featureName: String, isEnabled: Boolean) {
        repository.setFeature(featureName, isEnabled)
    }
}