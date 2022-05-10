package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.domain.entity.user.UserChanges

/**
 * Monitor global user updates for the current logged in user
 */
interface MonitorUserUpdates {
    /**
     * Invoke
     *
     * @return a flow of changes
     */
    operator fun invoke(): Flow<UserChanges>
}
