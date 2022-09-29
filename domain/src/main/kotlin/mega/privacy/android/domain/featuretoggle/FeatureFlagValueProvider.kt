package mega.privacy.android.domain.featuretoggle

import mega.privacy.android.domain.entity.Feature

/**
 * Feature flag value provider
 *
 */
interface FeatureFlagValueProvider {
    /**
     * Is enabled
     *
     * @param feature
     * @return Whether the feature is enabled or null if not set or found
     */
    suspend fun isEnabled(feature: Feature): Boolean?
}