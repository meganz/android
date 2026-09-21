package mega.privacy.android.domain.usecase.featureflag

import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.repository.FeatureFlagRepository
import javax.inject.Inject

/**
 * Get feature flag value
 *
 */
class GetFeatureFlagValueUseCase @Inject constructor(private val featureFlagRepository: FeatureFlagRepository) {
    /**
     * Invoke
     *
     * @param feature
     * @return value of the feature flag or false if no value found
     */
    suspend operator fun invoke(feature: Feature) =
        featureFlagRepository.getFeatureValue(feature) ?: false
}