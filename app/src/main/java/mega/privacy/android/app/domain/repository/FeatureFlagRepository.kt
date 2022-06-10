package mega.privacy.android.app.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.domain.entity.FeatureFlag

/**
 * Repository for feature flags
 */

interface FeatureFlagRepository {

    /**
     * Sets feature value to true or false
     *
     * @param featureName : name of the feature
     * @param isEnabled: Boolean value
     */
    suspend fun setFeature(featureName: String, isEnabled: Boolean)

    /**
     * Gets all features
     *
     * @return Flow of List of @FeatureFlag
     */
    suspend fun getAllFeatures(): Flow<MutableList<FeatureFlag>>
}