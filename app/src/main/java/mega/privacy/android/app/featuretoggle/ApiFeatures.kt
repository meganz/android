package mega.privacy.android.app.featuretoggle

import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.entity.featureflag.ApiFeature
import mega.privacy.android.domain.featuretoggle.FeatureFlagValueProvider

/**
 * Remote Api features
 *
 * @property experimentName Name of the AB test flag which we get from aPI team
 * @property description
 * @property defaultValue
 * @property checkRemote If true, the value will be checked from the remote server, if set to false we can toggle the flag as usual feature flag from the Settings in QA build
 */
enum class ApiFeatures(
    override val experimentName: String,
    override val description: String,
    private val defaultValue: Boolean,
    override val checkRemote: Boolean = true,
) : ApiFeature {
    /**
     * Call unlimited for pro users
     */
    CallUnlimitedProPlan(
        "chmon",
        "Call to stay unlimited when host with pro plan leaves",
        false
    ),

    /**
     * Enable Google ads with feature flag "ff_adse" or A/B test flag "ab_adse"
     */
    GoogleAdsFeatureFlag(
        "adse",
        "Enable Google Ads",
        false
    ),
    ;

    companion object : FeatureFlagValueProvider {
        override suspend fun isEnabled(feature: Feature) =
            entries.firstOrNull { it == feature }?.defaultValue
    }
}
