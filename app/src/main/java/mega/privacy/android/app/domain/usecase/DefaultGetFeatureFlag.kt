package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import mega.privacy.android.app.domain.repository.FeatureFlagRepository
import javax.inject.Inject

/**
 * Implementation of @GetFeatureFlag use case
 *
 * @param repository: Repository which will return the value
 */
class DefaultGetFeatureFlag @Inject constructor(val repository: FeatureFlagRepository) :
    GetFeatureFlag {

    /**
     * Invoke.
     *
     * @return flow of boolean value
     */
    override fun invoke(key: String): Flow<Boolean> {
        return flowOf(false)
    }
}