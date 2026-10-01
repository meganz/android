package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.NotificationsRepository
import javax.inject.Inject

/**
 *  Retrieve the push notification settings from the server and update the data in memory
 */
class UpdatePushNotificationSettingsUseCase @Inject constructor(
    private val notificationsRepository: NotificationsRepository,
) {
    /**
     * Invoke the use case
     */
    suspend operator fun invoke() =
        notificationsRepository.updatePushNotificationSettings()
}
