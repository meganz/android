package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.contacts.OnlineStatus

/**
 * Use case for monitoring updates on chat online statuses.
 */
fun interface MonitorOnlineStatusUpdates {

    /**
     * Invoke.
     *
     * @return Flow of [OnlineStatus].
     */
    operator fun invoke(): Flow<OnlineStatus>
}