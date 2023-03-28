package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * IsParticipatingInChatCallUseCase
 * Checks if current user is participating in any chat calls
 */
class IsParticipatingInChatCallUseCase @Inject constructor(private val callRepository: CallRepository) {

    /**
     * Invoke
     * @return true if user is participating in any call
     *         false if user is not participating in any calls
     */
    suspend operator fun invoke(): Boolean = with(callRepository) {
        return getCallHandleList(ChatCallStatus.Initial).isNotEmpty()
                || getCallHandleList(ChatCallStatus.Connecting).isNotEmpty()
                || getCallHandleList(ChatCallStatus.Joining).isNotEmpty()
                || getCallHandleList(ChatCallStatus.InProgress).isNotEmpty()
    }
}