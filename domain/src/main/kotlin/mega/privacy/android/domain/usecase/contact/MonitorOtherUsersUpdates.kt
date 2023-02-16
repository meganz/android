package mega.privacy.android.domain.usecase.contact

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.user.UserUpdate

/**
 * Monitor other users updates
 *
 * @constructor Create empty Monitor other users updates
 */
fun interface MonitorOtherUsersUpdates {
    /**
     * Invoke
     *
     * @return a flow of UserUpdate
     */
    operator fun invoke(): Flow<UserUpdate>
}
