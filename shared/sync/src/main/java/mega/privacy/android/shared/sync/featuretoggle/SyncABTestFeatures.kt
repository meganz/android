package mega.privacy.android.shared.sync.featuretoggle

import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.entity.featureflag.ABTestFeature
import mega.privacy.android.domain.featuretoggle.FeatureFlagValueProvider

/**
 * Sync features
 *
 * @property description
 * @property defaultValue
 *
 * Note: Please register your feature flag to the top of the list to minimize git-diff changes.
 */
enum class SyncABTestFeatures(
    override val experimentName: String,
    override val description: String,
    private val defaultValue: Boolean,
    override val checkRemote: Boolean = true,
) : ABTestFeature {

    /**
     * To use remote feature flag 'ab_async' from API
     * this flag activates Android Sync in Device Center
     */
    asyc(
        experimentName = "asyc",
        description = "Enable a synchronization between folders on local storage and folders on MEGA cloud",
        false
    );

    companion object : FeatureFlagValueProvider {
        override suspend fun isEnabled(feature: Feature) =
            entries.firstOrNull { it == feature }?.defaultValue
    }
}