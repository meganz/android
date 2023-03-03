package mega.privacy.android.domain.usecase.meeting

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.CallsSoundNotifications

/**
 * Gets calls sound notifications preference.
 *
 */
fun interface GetCallsSoundNotifications {

    /**
     * Invoke.
     *
     * @return Calls sound notifications.
     */
    operator fun invoke(): Flow<CallsSoundNotifications>
}