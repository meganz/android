package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Get Chat Room Preference Use Case
 *
 */
class MonitorChatRoomPreferenceUseCase @Inject constructor(
    private val repository: ChatRepository,
) {
    /**
     * Invoke
     *
     */
    operator fun invoke(chatId: Long) =
        repository.monitorChatRoomPreference(chatId)
}