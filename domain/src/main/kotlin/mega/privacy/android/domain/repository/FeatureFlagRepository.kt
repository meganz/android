package mega.privacy.android.domain.repository

import mega.privacy.android.domain.entity.Feature

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
}