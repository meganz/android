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
    ),

    EnableUSExternalBillingForEligibleUsers(
        "aepay",
        "Enables US external billing for eligible users",
        false
    ),

    /**
     * Controls whether Android 16+ orientation migration is enabled.
     *
     * When enabled (true):
     * - Uses adaptive orientation handling for Android 16+ devices
     * - Ignores fixed orientation settings on large screen devices
     * - Allows system to handle orientation changes automatically
     *
     * When disabled (false):
     * - Uses legacy orientation behavior
     * - Maintains existing fixed orientation settings
     * - Preserves backward compatibility with older Android versions
     *
     * Default: false (gradual rollout for safety)
     */
    Android16OrientationMigrationEnabled(
        experimentName = "aome2",
        description = "Enable Android 16+ orientation migration for large screen devices",
        defaultValue = false
    ),

    /**
     * Age Signal Check feature flag
     *
     * Controls whether the app checks the user's age signal to hide Stripe payment method
     * if the user is under the allowed age. When enabled, Stripe as a payment option
     * is not shown to users under age according to Google's Age Signals API.
     *
     * Default: false
     */
    AgeSignalsCheckEnabled(
        experimentName = "ages1",
        description = "Do not show Stripe payment method if it is under age",
        defaultValue = false
    ),

    /**
     * Flag to allow multiple selection for favorite/label in Cloud Drive.
     * When enabled, users can select multiple favorites or labels at once.
     *
     * Default: false
     */
    AllowMultipleSelectionsEnabled(
        experimentName = "mult1",
        description = "Allow multiple selection for favorite/label in Cloud Drive",
        defaultValue = false
    ),

    /**
     * Media Revamp phase 2 feature flag
     */
    MediaRevampPhase2(
        experimentName = "mrp2",
        description = "Enable Media Revamp phase 2 features",
        defaultValue = false
    ),
    ;

    companion object : FeatureFlagValueProvider {
        override suspend fun isEnabled(feature: Feature) =
            entries.firstOrNull { it == feature }?.defaultValue

        override val priority: FeatureFlagValuePriority = FeatureFlagValuePriority.Default
    }
}
