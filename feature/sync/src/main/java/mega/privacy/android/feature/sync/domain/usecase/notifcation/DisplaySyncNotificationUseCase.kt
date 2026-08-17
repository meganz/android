package mega.privacy.android.feature.sync.domain.usecase.notifcation

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.domain.repository.SyncNotificationRepository
import mega.privacy.android.feature.sync.ui.notification.SyncNotificationManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Displays a sync notification exactly once per notification identity in this process.
 *
 * Multiple observers can request the same notification at the same time. The mutex and persisted
 * identity check make the post-and-record operation safe without treating the whole notification
 * channel as a global lock.
 */
@Singleton
class DisplaySyncNotificationUseCase @Inject constructor(
    private val syncNotificationRepository: SyncNotificationRepository,
    private val syncNotificationManager: SyncNotificationManager,
    private val setSyncNotificationShownUseCase: SetSyncNotificationShownUseCase,
) {
    private val mutex = Mutex()

    suspend operator fun invoke(notification: SyncNotificationMessage): Boolean = mutex.withLock {
        if (syncNotificationRepository.isNotificationDisplayed(notification)) return false

        val notificationId = syncNotificationManager.show(notification) ?: return false
        try {
            setSyncNotificationShownUseCase(
                syncNotificationMessage = notification,
                notificationId = notificationId,
            )
            true
        } catch (exception: Exception) {
            syncNotificationManager.cancelNotification(notificationId)
            throw exception
        }
    }
}
