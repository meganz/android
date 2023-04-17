package mega.privacy.android.domain.usecase.setting

import mega.privacy.android.domain.repository.PushesRepository
import javax.inject.Inject

/**
 * MonitorPushNotificationsSettingsUseCase
 */
class MonitorUpdatePushNotificationSettingsUseCase @Inject constructor(private val pushesRepository: PushesRepository) {

    /**
     * Invoke
     *
     * @return flow [Boolean] flow is triggered whenever there is an update for push notification settings
     */
    operator fun invoke() = pushesRepository.monitorPushNotificationSettings()
}