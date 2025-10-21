package mega.privacy.android.app.features

import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.featuretoggle.FeatureFlagValuePriority
import mega.privacy.android.domain.featuretoggle.FeatureFlagValueProvider

/**
 * Feature flags for Android 16+ orientation migration compatibility.
 *
 * Starting with Android 16, Google requires apps to adapt to ignore fixed orientations
 * on large screen devices (tablets and foldables with smallest screen width ≥ 600dp).
 * This feature flag controls whether the app uses the new adaptive orientation handling
 * or maintains legacy behavior for backward compatibility.
 */
internal enum class OrientationMigrationFeature(
    override val description: String,
    private val defaultValue: Boolean,
) : Feature {

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
        description = "Enable Android 16+ orientation migration for large screen devices",
        defaultValue = false
    )
    ;

    companion object : FeatureFlagValueProvider {
        /**
         * Determines if the orientation migration feature is enabled.
         *
         * @param feature The feature to check
         * @return true if the feature is enabled, false otherwise
         */
        override suspend fun isEnabled(feature: Feature) =
            OrientationMigrationFeature.entries.firstOrNull { it == feature }?.defaultValue

        override val priority: FeatureFlagValuePriority = FeatureFlagValuePriority.Default
    }
}
