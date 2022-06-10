package mega.privacy.android.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mega.privacy.android.app.data.gateway.preferences.FeatureFlagPreferencesGateway
import mega.privacy.android.app.domain.entity.FeatureFlag
import mega.privacy.android.app.domain.repository.FeatureFlagRepository
import javax.inject.Inject

/**
 * Implementation of @FeatureFlagRepository
 *
 * @param preferencesGateway: Preference Gateway
 */
class DefaultFeatureFlagRepository @Inject constructor(
    private val preferencesGateway: FeatureFlagPreferencesGateway,
) : FeatureFlagRepository {

    /**
     * Sets feature value to true or false
     *
     * @param featureName : name of the feature
     * @param isEnabled: Boolean value
     */
    override suspend fun setFeature(featureName: String, isEnabled: Boolean) =
        preferencesGateway.setFeature(featureName, isEnabled)

    /**
     * Gets all features
     *
     * @return Flow of List of @FeatureFlag
     */
    override suspend fun getAllFeatures(): Flow<MutableList<FeatureFlag>> {
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