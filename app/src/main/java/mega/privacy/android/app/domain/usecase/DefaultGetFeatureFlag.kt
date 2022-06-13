package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import mega.privacy.android.app.domain.repository.FeatureFlagRepository
import javax.inject.Inject

class DefaultGetFeatureFlag @Inject constructor(val repository: FeatureFlagRepository): GetFeatureFlag {

    override fun invoke(key: String): Flow<Boolean> {
        return flowOf(false)
    }
}