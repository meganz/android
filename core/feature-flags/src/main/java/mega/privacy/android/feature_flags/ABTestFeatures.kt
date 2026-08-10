package mega.privacy.android.feature_flags

import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.entity.featureflag.ABTestFeature
import mega.privacy.android.domain.featuretoggle.FeatureFlagValuePriority
import mega.privacy.android.domain.featuretoggle.FeatureFlagValueProvider

/**
 * Remote features
 *
 * @property experimentName Name of the AB test flag which we get from aPI team without ab_ prefix
 * @property description
 * @property defaultValue
 * @property checkRemote If true, the value will be checked from the remote server, if set to false we can toggle the flag as usual feature flag from the Settings in QA build
 */
enum class ABTestFeatures(
    override val experimentName: String,
    override val description: String,
    private val defaultValue: Boolean,
    override val checkRemote: Boolean = true,
) : ABTestFeature {


    /**
     * [A/B Test] ab_ande - External Checkout Experiment
     *
     * This flag is for the "ab_ande" test.
     * Context: We're running an experiment to compare payment flows for US users.
     * The two variants are:
     *   - External checkout as default
     *   - In-app checkout as default
     * The goal is to determine which flow leads to a higher conversion rate.
     */
    ande(
        experimentName = "ande",
        description = "Use external checkout if true, otherwise false",
        defaultValue = false
    ),

    /**
     * To use remote feature flag 'ab_devtest' from API
     * this flag can be used to test the SDK methods getABTestValue() and sendABTestActive()
     * this flag can be used to test any A/B testing implementation before the API flag for real campaign will be created
     * this flag is not part of any existing A/B testing campaign
     * for real A/B testing campaign new flag will be created on API side and new enum type should be created here
     */
    devtest(
        experimentName = "devtest",
        description = "Remote feature flag from API for any tests related to A/B testing",
        defaultValue = false
    );

    companion object : FeatureFlagValueProvider {
        override suspend fun isEnabled(feature: Feature) =
            entries.firstOrNull { it == feature }?.defaultValue

        override val priority: FeatureFlagValuePriority = FeatureFlagValuePriority.Default
    }
}

