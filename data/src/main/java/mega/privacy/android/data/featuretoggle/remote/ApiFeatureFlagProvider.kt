package mega.privacy.android.data.featuretoggle.remote

import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.entity.featureflag.ApiFeature
import mega.privacy.android.domain.entity.featureflag.GroupFlagTypes
import mega.privacy.android.domain.featuretoggle.FeatureFlagValueProvider
import mega.privacy.android.domain.usecase.featureflag.GetFlagUseCase
import javax.inject.Inject

/**
 * API feature flag value provider
 *
 */
class ApiFeatureFlagProvider @Inject constructor(
    private val getFlagUseCase: GetFlagUseCase,
) : FeatureFlagValueProvider {
    override suspend fun isEnabled(feature: Feature): Boolean? {
        if (feature is ApiFeature) {
            if (feature.checkRemote) {
                return getFlagUseCase(feature.experimentName)?.let {
                    when (it.group) {
                        GroupFlagTypes.Disabled -> false
                        GroupFlagTypes.Enabled -> true
                    }
                }
            }
        }
        return null
    }
}
