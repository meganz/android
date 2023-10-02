package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Is Meeting Ended Use Case
 *
 */
class IsMeetingEndedUseCase @Inject constructor(
    private val repository: ChatRepository,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(chatOptionsBitMask: Int, handles: List<Long>?): Boolean =
        !repository.hasWaitingRoomChatOptions(chatOptionsBitMask)
                && (handles.isNullOrEmpty() || handles.first() == repository.getChatInvalidHandle())
}