package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.FeatureFlagRepository
import javax.inject.Inject

class DefaultSetFeatureFlag @Inject constructor(private val repository: FeatureFlagRepository) : SetFeatureFlag {

    override suspend fun invoke(featureName: String, isEnabled: Boolean) {
        repository.setFeature(featureName, isEnabled)
    }
}