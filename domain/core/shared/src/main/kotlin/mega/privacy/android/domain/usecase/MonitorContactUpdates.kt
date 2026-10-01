package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.user.UserUpdate

/**
 * User case for monitoring contact updates.
 */
fun interface MonitorContactUpdates {

    /**
     * Invoke.
     *
     * @return Flow of [UserUpdate].
     */
    operator fun invoke(): Flow<UserUpdate>
}