package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * IsParticipatingInChatCallUseCase
 * Checks if current user is participating in any chat calls
 */
class GetCurrentChatCallUseCase @Inject constructor(private val callRepository: CallRepository) {

    /**
     * Invoke
     * @return chat id if user is participating in any call
     *         null if user is not participating in any calls
     */
    suspend operator fun invoke(): Long? = with(callRepository) {
        return (getCallHandleList(ChatCallStatus.Initial).firstOrNull()
            ?: getCallHandleList(ChatCallStatus.Connecting).firstOrNull()
            ?: getCallHandleList(ChatCallStatus.Joining).firstOrNull()
            ?: getCallHandleList(ChatCallStatus.InProgress).firstOrNull())
    }
}