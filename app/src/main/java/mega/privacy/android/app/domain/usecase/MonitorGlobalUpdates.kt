package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.data.model.GlobalUpdate

/**
 * Monitor global updates for the current logged in user
 */
interface MonitorGlobalUpdates {
    /**
     * Invoke
     *
     * @return a flow of [GlobalUpdate]
     */
    @Deprecated("See GlobalUpdatesRepository for individual replacements to use instead.")
    operator fun invoke(): Flow<GlobalUpdate>
}
