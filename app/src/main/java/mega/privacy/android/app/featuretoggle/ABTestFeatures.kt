package mega.privacy.android.app.featuretoggle

import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.entity.featureflag.ABTestFeature
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
    ),

    /**
     * To use remote feature flag 'ab_ads' from API
     * this flag is part of real experiment related to Ads
     * DO NOT USE this flag anywhere else, except the Ads related files
     */
    ads(
        experimentName = "ads",
        description = "Real experiment flag to show ads",
        defaultValue = false
    ),

    /**
     * To use remote feature flag 'ab_adse' from API
     * this flag is part of real experiment related to Ads
     * DO NOT USE this flag anywhere else, except the Ads related files
     */
    adse(
        experimentName = "adse",
        description = "Real experiment flag to show external ads",
        defaultValue = false
    ),

    /**
     * Enable new design Variant A for ChooseAccount screen
     */
    ChooseAccountScreenVariantA(
        experimentName = "obusd",
        description = "Enable new design (Variant A) for ChooseAccount screen (Onboarding Upselling dialog)",
        defaultValue = false,
        checkRemote = true,
    ) {
        override fun mapValue(input: Long): Boolean = when (input) {
            1L -> true
            else -> false
        }
    },


    /**
     * Enable new design Variant B for ChooseAccount screen
     */
    ChooseAccountScreenVariantB(
        experimentName = "obusd",
        description = "Enable new design (Variant B) for ChooseAccount screen (Onboarding Upselling dialog)",
        defaultValue = false,
        checkRemote = true,
    ) {
        override fun mapValue(input: Long): Boolean = when (input) {
            2L -> true
            else -> false
        }
    }
    ;

    companion object : FeatureFlagValueProvider {
        override suspend fun isEnabled(feature: Feature) =
            entries.firstOrNull { it == feature }?.defaultValue
    }
}

