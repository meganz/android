package mega.privacy.android.domain.usecase.setting

import mega.privacy.android.domain.repository.PushesRepository
import javax.inject.Inject

/**
 * BroadcastPushNotificationSettingsUseCase
 */
class BroadcastPushNotificationSettingsUseCase @Inject constructor(private val pushesRepository: PushesRepository) {

    /**
     * Invoke
     *
     * Updates the shared flow to trigger push notification updated
     */
    suspend operator fun invoke() = pushesRepository.broadcastPushNotificationSettings()
}