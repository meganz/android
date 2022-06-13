package mega.privacy.android.app.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.domain.entity.FeatureFlag

interface FeatureFlagRepository {

    suspend fun setFeature(featureName: String, isEnabled: Boolean)

    suspend fun getAllFeatures(): Flow<MutableList<FeatureFlag>>
}