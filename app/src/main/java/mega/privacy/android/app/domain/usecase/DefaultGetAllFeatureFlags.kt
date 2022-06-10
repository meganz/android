package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.domain.entity.FeatureFlag
import mega.privacy.android.app.domain.repository.FeatureFlagRepository
import javax.inject.Inject

/**
 * Implementation of @GetAllFeatureFlags use case
 */
class DefaultGetAllFeatureFlags @Inject constructor(private val repository: FeatureFlagRepository) :
    GetAllFeatureFlags {

    /**
     * Invoke.
     *
     * @return Flow of list of @FeatureFlag
     */
    override suspend fun invoke(): Flow<MutableList<FeatureFlag>> {
        return repository.getAllFeatures()
    }
}