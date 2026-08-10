package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.domain.entity.chat.ChatNotificationMuteState
import mega.privacy.android.domain.usecase.setting.MonitorUpdatePushNotificationSettingsUseCase
import javax.inject.Inject

/**
 * Monitor the notification mute state of a chat room, emitting the current state first and
 * then recomputing it on every push notification settings update.
 */
class MonitorChatNotificationMuteStateUseCase @Inject constructor(
    private val isChatNotificationMuteUseCase: IsChatNotificationMuteUseCase,
    private val getChatDoNotDisturbTimeUseCase: GetChatDoNotDisturbTimeUseCase,
    private val monitorUpdatePushNotificationSettingsUseCase: MonitorUpdatePushNotificationSettingsUseCase,
) {
    /**
     * Invoke.
     *
     * @param chatId Id of the chat room.
     * @return Flow of [ChatNotificationMuteState].
     */
    operator fun invoke(chatId: Long): Flow<ChatNotificationMuteState> =
        monitorUpdatePushNotificationSettingsUseCase()
            .onStart { emit(true) }
            .map { getMuteState(chatId) }

    private suspend fun getMuteState(chatId: Long): ChatNotificationMuteState {
        val isMuted = isChatNotificationMuteUseCase(chatId)
        val mutedUntilTimestamp = if (isMuted) {
            getChatDoNotDisturbTimeUseCase(chatId).takeIf { it > 0 }
        } else {
            null
        }
        return ChatNotificationMuteState(
            isMuted = isMuted,
            mutedUntilTimestamp = mutedUntilTimestamp,
        )
    }
}
