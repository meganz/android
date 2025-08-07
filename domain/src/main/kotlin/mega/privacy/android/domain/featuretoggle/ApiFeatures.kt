package mega.privacy.android.domain.featuretoggle

import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.entity.featureflag.ApiFeature

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

    /**
     * Enable hidden nodes for internal release
     */
    HiddenNodesInternalRelease(
        experimentName = "hnir",
        description = "Enable hidden nodes for internal release",
        defaultValue = false,
        checkRemote = true,
    ),

    /**
     * Note to yourself feature flag
     */
    NoteToYourselfFlag(
        "n2s",
        "Enable note to yourself",
        false
    ),

    /**
     * Migration to mega app domain.
     */
    MegaDotAppDomain(
        "site",
        "Enable migration to mega app domain",
        false
    );

    companion object : FeatureFlagValueProvider {
        override suspend fun isEnabled(feature: Feature) =
            entries.firstOrNull { it == feature }?.defaultValue

        override val priority: FeatureFlagValuePriority = FeatureFlagValuePriority.Default
    }
}