package mega.privacy.android.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mega.privacy.android.app.data.gateway.preferences.FeatureFlagPreferencesGateway
import mega.privacy.android.app.domain.entity.FeatureFlag
import mega.privacy.android.app.domain.repository.FeatureFlagRepository
import javax.inject.Inject

class DefaultFeatureFlagRepository @Inject constructor(
        private val preferencesGateway: FeatureFlagPreferencesGateway
) : FeatureFlagRepository {

    override suspend fun setFeature(featureName: String, isEnabled: Boolean) =
            preferencesGateway.setFeature(featureName, isEnabled)

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