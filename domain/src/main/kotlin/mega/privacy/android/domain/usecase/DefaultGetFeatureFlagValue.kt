package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.repository.FeatureFlagRepository
import javax.inject.Inject

/**
 * Default implementation of [GetFeatureFlagValue]
 *
 * @property featureFlagRepository
 */
class DefaultGetFeatureFlagValue @Inject constructor(private val featureFlagRepository: FeatureFlagRepository) :
    GetFeatureFlagValue {
    override suspend fun invoke(feature: Feature) =
        featureFlagRepository.getFeatureValue(feature) ?: false

}
