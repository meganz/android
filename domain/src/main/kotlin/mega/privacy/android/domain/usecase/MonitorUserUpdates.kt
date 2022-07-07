package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.user.UserChanges

/**
 * Monitor global user updates for the current logged in user
 */
fun interface MonitorUserUpdates {
    /**
     * Invoke
     *
     * @return a flow of changes
     */
    operator fun invoke(): Flow<UserChanges>
}
