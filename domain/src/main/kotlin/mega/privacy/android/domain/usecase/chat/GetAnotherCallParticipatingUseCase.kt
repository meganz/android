package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.repository.CallRepository
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Get Another Call Participating Use Case
 *
 */
class GetAnotherCallParticipatingUseCase @Inject constructor(
    private val callRepository: CallRepository,
    private val chatRepository: ChatRepository,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(chatId: Long): Long {
        listOf(ChatCallStatus.InProgress, ChatCallStatus.Joining, ChatCallStatus.Connecting)
            .forEach {
                callRepository.getCallHandleList(it).find { id -> id != chatId }
                    ?.let { id ->
                        return id
                    }
            }
        return chatRepository.getChatInvalidHandle()
    }
}