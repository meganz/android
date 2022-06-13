package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.domain.entity.FeatureFlag

interface GetAllFeatureFlags {

    suspend operator fun invoke(): Flow<MutableList<FeatureFlag>>
}