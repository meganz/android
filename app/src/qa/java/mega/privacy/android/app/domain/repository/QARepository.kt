package mega.privacy.android.app.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.entity.Progress

/**
 * Qa repository
 *
 * Provides QA related functionality
 */
interface QARepository {
    /**
     * Update app from QA distribution channel
     *
     * @return Download progress as a float
     */
    fun updateApp(): Flow<Progress>

    /**
     * Sets value of feature flag
     *
     * @param featureName: Name of the feature
     * @param isEnabled: Boolean value
     */
    suspend fun setFeature(featureName: String, isEnabled: Boolean)

    /**
     * Get all features
     *
     * @return a list of all defined Feature toggles
     */
    suspend fun getAllFeatures(): List<Feature>

    /**
     * Monitor local feature flags
     *
     * @return a flow containing a map of all local feature names and their current value
     */
    fun monitorLocalFeatureFlags(): Flow<Map<String, Boolean>>

}