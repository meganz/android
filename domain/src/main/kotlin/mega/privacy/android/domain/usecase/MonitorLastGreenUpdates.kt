package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.user.UserLastGreen

/**
 * Use case for monitoring updates on last green.
 */
fun interface MonitorLastGreenUpdates {

    /**
     * Invoke.
     *
     * @return Flow of [UserLastGreen].
     */
    operator fun invoke(): Flow<UserLastGreen>
}