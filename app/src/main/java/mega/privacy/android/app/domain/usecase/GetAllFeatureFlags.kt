package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.domain.entity.FeatureFlag

/**
 * Use case to get all feature flags
 */
interface GetAllFeatureFlags {

    /**
     * Gets a fow of list of all feature flags
     * @return: Flow of List of @FeatureFlag
     */
    suspend operator fun invoke(): Flow<MutableList<FeatureFlag>>
}