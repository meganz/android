package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.domain.entity.FeatureFlag
import mega.privacy.android.app.domain.repository.FeatureFlagRepository
import javax.inject.Inject

/**
 * Implementation of GetAllFeatureFlags
 * @param repository: Feature flag repository
 */
class DefaultGetAllFeatureFlags @Inject constructor(private val repository: FeatureFlagRepository) :
    GetAllFeatureFlags {

    /**
     * Gets a fow of list of all feature flags
     * @return: Flow of List of @FeatureFlag
     */
    override suspend fun invoke(): Flow<MutableList<FeatureFlag>> {
        return repository.getAllFeatures()
    }
}