package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow

/**
 * Use case to get a single feature flag value
 */
interface GetFeatureFlag {

    /**
     * Invoke
     *
     * @return flow of boolean value
     */
    operator fun invoke(key: String): Flow<Boolean>
}