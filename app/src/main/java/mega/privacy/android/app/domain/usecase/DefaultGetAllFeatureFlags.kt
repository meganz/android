package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.domain.entity.FeatureFlag
import mega.privacy.android.app.domain.repository.FeatureFlagRepository
import timber.log.Timber
import javax.inject.Inject

class DefaultGetAllFeatureFlags @Inject constructor(private val repository: FeatureFlagRepository) : GetAllFeatureFlags {

    override suspend fun invoke(): Flow<MutableList<FeatureFlag>> {
        return repository.getAllFeatures()
    }
}