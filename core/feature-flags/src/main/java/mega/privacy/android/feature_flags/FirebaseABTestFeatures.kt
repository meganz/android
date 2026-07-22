package mega.privacy.android.feature_flags

import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.entity.featureflag.FirebaseABTestFeature
import mega.privacy.android.domain.featuretoggle.FeatureFlagValuePriority
import mega.privacy.android.domain.featuretoggle.FeatureFlagValueProvider

/**
 * Firebase Remote Config driven A/B test features. Use it only when you want to run experiments
 * to measure retention and revenue impact.
 *
 * Each entry maps to a Remote Config parameter defined in the Firebase console. The value
 * served to a user is controlled by a Firebase A/B Testing experiment on that parameter;
 * [defaultValue] applies until a remotely fetched value is available.
 *
 * @property remoteConfigKey Key of the Remote Config parameter in the Firebase console
 * @property description
 * @property defaultValue
 */
enum class FirebaseABTestFeatures(
    override val remoteConfigKey: String,
    override val description: String,
    private val defaultValue: Boolean,
) : FirebaseABTestFeature {

    /**
     * [A/B Test] show_paywall_after_signup
     *
     * Controls whether the upgrade/paywall screen is shown right after signup completes.
     * The goal is to measure the impact on revenue and retention.
     */
    ShowPaywallAfterSignup(
        remoteConfigKey = "show_paywall_after_signup",
        description = "Show the upgrade/paywall screen right after signup completes",
        defaultValue = false,
    );

    companion object : FeatureFlagValueProvider {
        override suspend fun isEnabled(feature: Feature) =
            entries.firstOrNull { it == feature }?.defaultValue

        override val priority: FeatureFlagValuePriority = FeatureFlagValuePriority.Default
    }
}
