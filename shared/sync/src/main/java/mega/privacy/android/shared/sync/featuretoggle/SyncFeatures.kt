package mega.privacy.android.shared.sync.featuretoggle

import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.featuretoggle.FeatureFlagValueProvider

/**
 * Sync features
 *
 * @property description
 * @property defaultValue
 *
 * Note: Please register your feature flag to the top of the list to minimize git-diff changes.
 */
enum class SyncFeatures(
    override val description: String,
    private val defaultValue: Boolean,
) : Feature {

    /**
     * Android Sync toggle
     */
    AndroidSync(
        description = "Enable a synchronization between folders on local storage and folders on MEGA cloud",
        defaultValue = false,
    );

    companion object : FeatureFlagValueProvider {
        override suspend fun isEnabled(feature: Feature) =
            entries.firstOrNull { it == feature }?.defaultValue
    }
}