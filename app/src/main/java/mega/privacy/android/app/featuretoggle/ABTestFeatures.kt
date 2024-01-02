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
     * To use remote feature flag 'ab_sus2023' from API
     * this flag is part of real experiment related to Upgrade account screen
     * DO NOT USE this flag anywhere else, except the Upgrade account screen
     * criteria: every day 25% of users will be assigned to see either LegacyUpgradeAccountView or UpgradeAccountView (12.5% each respectively)
     */
    sus2023(
        "Real experiment flag for Upgrade account screen",
        false
    ),

    /**
     * To use remote feature flag 'ab_nsf' from API
     * this flag is part of real experiment related to Search screen
     * DO NOT USE this flag anywhere else, except the Search screen
     */
    nsf(
        "Real experiment flag to show filters on search",
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
    );

    companion object : FeatureFlagValueProvider {
        override suspend fun isEnabled(feature: Feature) =
            entries.firstOrNull { it == feature }?.defaultValue
    }
}

