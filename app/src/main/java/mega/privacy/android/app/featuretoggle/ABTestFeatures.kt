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
    ),

    /**
     * To use remote feature flag 'ab_ads' from API
     * this flag is part of real experiment related to Ads
     * DO NOT USE this flag anywhere else, except the Ads related files
     */
    ads(
        "Real experiment flag to show ads",
        false
    ),

    /**
     * To use remote feature flag 'ab_adse' from API
     * this flag is part of real experiment related to Ads
     * DO NOT USE this flag anywhere else, except the Ads related files
     */
    adse(
        "Real experiment flag to show external ads",
        false
    ),

    /**
     * To use remote feature flag 'ab_dmca' from API
     * This flag activates the Device Center tab and removes the Backups tab
     */
    dmca(
        "Remote feature flag to show Device Center",
        false
    );

    companion object : FeatureFlagValueProvider {
        override suspend fun isEnabled(feature: Feature) =
            entries.firstOrNull { it == feature }?.defaultValue
    }
}

