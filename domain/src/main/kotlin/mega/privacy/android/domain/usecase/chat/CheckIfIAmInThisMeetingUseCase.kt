package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Check In This Meeting Use Case
 *
 */
class CheckIfIAmInThisMeetingUseCase @Inject constructor(
    private val callRepository: CallRepository,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(chatId: Long): Boolean {
        val call = callRepository.getChatCall(chatId)
        return call != null && call.status != ChatCallStatus.Destroyed &&
                call.status != ChatCallStatus.TerminatingUserParticipation &&
                call.status != ChatCallStatus.UserNoPresent
    }
}