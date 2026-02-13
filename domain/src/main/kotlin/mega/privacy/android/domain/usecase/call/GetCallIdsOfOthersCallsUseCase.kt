package mega.privacy.android.domain.usecase.call

import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Get all current call ids of others calls that I'm participating
 */
class GetCallIdsOfOthersCallsUseCase @Inject constructor(private val callRepository: CallRepository) {

    /**
     * Invoke
     *
     * @param currentChatId Current chat id, for filtering calls in other chats.
     * @return list of calls if user has call in progress, empty otherwise.
     */
    suspend operator fun invoke(currentChatId: Long): List<Long> = with(callRepository) {
        buildList {
            addAll(getCallChatIdList(ChatCallStatus.Initial))
            addAll(getCallChatIdList(ChatCallStatus.Connecting))
            addAll(getCallChatIdList(ChatCallStatus.Joining))
            addAll(getCallChatIdList(ChatCallStatus.InProgress))
        }.filter { it != currentChatId }
    }
}