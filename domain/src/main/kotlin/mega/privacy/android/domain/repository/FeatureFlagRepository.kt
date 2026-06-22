package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.featuretoggle.FeatureFlagValuePriority

/**
 * Repository for feature flag
 */
interface FeatureFlagRepository {

    /**
     * Get feature value
     *
     * @param feature
     * @return the value of the feature flag if found
     */
    suspend fun getFeatureValue(feature: Feature): Boolean?

    /**
     * Get feature value
     *
     * @param feature
     * @param priorities set of priorities to filter the providers
     * @return the value of the feature flag if found
     */
    suspend fun getFeatureValue(
        feature: Feature,
        priorities: Set<FeatureFlagValuePriority>,
    ): Boolean?

    /**
     * Get current persisted snapshot
     *
     * @return current persisted snapshot
     */
    suspend fun getCurrentPersistedSnapshot(): Map<Feature, Boolean>

    /**
     * Apply snapshot
     *
     * @param newSnapshot
     */
    suspend fun applySnapshot(newSnapshot: Map<Feature, Boolean>)

    /**
     * Clear the persisted feature flag snapshot.
     */
    suspend fun clearPersistedSnapshot()
}