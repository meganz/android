package mega.privacy.android.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mega.privacy.android.app.data.gateway.preferences.FeatureFlagPreferencesGateway
import mega.privacy.android.app.domain.entity.FeatureFlag
import mega.privacy.android.app.domain.repository.FeatureFlagRepository
import javax.inject.Inject

/**
 * Implementation of @FeatureFlagRepository
 * @param preferencesGateway: Data Store Preferences gateway
 */
class DefaultFeatureFlagRepository @Inject constructor(
    private val preferencesGateway: FeatureFlagPreferencesGateway,
) : FeatureFlagRepository {

    /**
     * Sets value of feature flag
     *
     * @param featureName: Name of the feature
     * @param isEnabled: Boolean value
     */
    override suspend fun setFeature(featureName: String, isEnabled: Boolean) =
        preferencesGateway.setFeature(featureName, isEnabled)

    /**
     * Gets a fow of list of all feature flags
     * @return: Flow of List of @FeatureFlag
     */
    override suspend fun getAllFeatures(): Flow<List<FeatureFlag>> {
        return flow {
            val list = mutableListOf<FeatureFlag>()
            preferencesGateway.getAllFeatures().collect { map ->
                map.keys.forEach { key ->
                    list.add(FeatureFlag(key, map[key] ?: false))
                }
                emit(list)
            }
        }
    }
}