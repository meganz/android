package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.CallsSoundNotifications

/**
 * Sets calls sound notifications preference.
 */
fun interface SetCallsSoundNotifications {

    /**
     * Invoke.
     *
     * @param soundNotificationsStatus sound notification.
     * @return Status of calls sound notifications.
     */
    suspend operator fun invoke(soundNotificationsStatus: CallsSoundNotifications)
}