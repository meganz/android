package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Get all current call ids in other chats use case
 */
class GetCurrentCallIdsInOtherChatsUseCase @Inject constructor(private val callRepository: CallRepository) {

    /**
     * Invoke
     *
     * @param currentChatId Current chat id, for filtering calls in other chats.
     * @return list of calls if user has any active call in other chats, empty otherwise.
     */
    suspend operator fun invoke(currentChatId: Long): List<Long> = with(callRepository) {
        buildList {
            addAll(getCallHandleList(ChatCallStatus.Initial))
            addAll(getCallHandleList(ChatCallStatus.UserNoPresent))
            addAll(getCallHandleList(ChatCallStatus.Connecting))
            addAll(getCallHandleList(ChatCallStatus.Joining))
            addAll(getCallHandleList(ChatCallStatus.InProgress))
        }.filter { it != currentChatId }
    }
}