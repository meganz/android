package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.domain.repository.NotificationsRepository
import mega.privacy.android.domain.usecase.setting.MonitorUpdatePushNotificationSettingsUseCase
import javax.inject.Inject

/**
 * Monitor whether Do-Not-Disturb mode is enabled for all chats, emitting the current state first
 * and then recomputing it on every push notification settings update.
 */
class MonitorChatsDoNotDisturbEnabledUseCase @Inject constructor(
    private val notificationsRepository: NotificationsRepository,
    private val monitorUpdatePushNotificationSettingsUseCase: MonitorUpdatePushNotificationSettingsUseCase,
) {
    /**
     * Invoke.
     *
     * @return Flow emitting true while every chat is silenced, false otherwise.
     */
    operator fun invoke(): Flow<Boolean> =
        monitorUpdatePushNotificationSettingsUseCase()
            .onStart { emit(true) }
            .map { notificationsRepository.isChatsDoNotDisturbEnabled() }
}
