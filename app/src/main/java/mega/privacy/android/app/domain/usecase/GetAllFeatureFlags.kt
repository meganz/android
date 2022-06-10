package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.domain.entity.FeatureFlag

/**
 * Gets all feature flags
 */
interface GetAllFeatureFlags {

    /**
     * Invoke.
     * @return Flow of list of @FeatureFlag
     */
    suspend operator fun invoke(): Flow<MutableList<FeatureFlag>>
}