package mega.privacy.android.app.data.gateway.preferences

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.domain.entity.FeatureFlag

interface FeatureFlagPreferencesGateway {

    suspend fun setFeature(featureName: String, isEnabled: Boolean)

    suspend fun getAllFeatures(): Flow<Map<String, Boolean>>
}