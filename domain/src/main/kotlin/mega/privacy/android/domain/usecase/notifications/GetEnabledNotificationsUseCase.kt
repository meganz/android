package mega.privacy.android.domain.usecase.notifications

import mega.privacy.android.domain.repository.NotificationsRepository
import javax.inject.Inject

/**
 * Get list of Enabled Notifications IDs
 */
class GetEnabledNotificationsUseCase @Inject constructor(
    private val notificationsRepository: NotificationsRepository,
) {
    /**
     * Invoke
     *
     * @return [List<Int>]
     */
    suspend operator fun invoke(): List<Int> =
        notificationsRepository.getEnabledNotifications()
}