package mega.privacy.android.shared.sync.featuretoggles

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
     * Android Sync Work Manager feature flag.
     *
     * Activate this flag if you want to use WorkManager instead of SyncBackgroundService
     */
    AndroidSyncWorkManager(
        "Enable the new Android Sync WorkManager to run instead of Service",
        false
    );

    companion object : FeatureFlagValueProvider {
        override suspend fun isEnabled(feature: Feature) =
            entries.firstOrNull { it == feature }?.defaultValue
    }
}