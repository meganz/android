package mega.privacy.android.app.featuretoggle

import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.entity.featureflag.ABTestFeature
import mega.privacy.android.domain.featuretoggle.FeatureFlagValueProvider

/**
 * Remote features
 *
 * @property description
 * @property defaultValue
 */
enum class ABTestFeatures(
    override val description: String,
    private val defaultValue: Boolean,
) : ABTestFeature {
    /**
     * To use remote feature flag 'ab_devtest' from API
     * this flag can be used to test the SDK methods getABTestValue() and sendABTestActive()
     * this flag can be used to test any A/B testing implementation before the API flag for real campaign will be created
     * this flag is not part of any existing A/B testing campaign
     * for real A/B testing campaign new flag will be created on API side and new enum type should be created here
     */
    devtest(
        "Remote feature flag from API for any tests related to A/B testing",
        false
    );

    companion object : FeatureFlagValueProvider {
        override suspend fun isEnabled(feature: Feature) =
            ABTestFeatures.values().firstOrNull { it == feature }?.defaultValue
    }
}

