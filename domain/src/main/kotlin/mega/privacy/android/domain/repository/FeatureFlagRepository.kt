package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.Feature

/**
 * Repository for feature flag
 */
interface FeatureFlagRepository {

    /**
     * Sets value of feature flag
     *
     * @param featureName: Name of the feature
     * @param isEnabled: Boolean value
     */
    suspend fun setFeature(featureName: String, isEnabled: Boolean)

    /**
     * Gets a fow of list of all feature flags
     * @return: Flow of List of @FeatureFlag
     */
    fun getAllFeatures(): Flow<Map<Feature, Boolean>>
}