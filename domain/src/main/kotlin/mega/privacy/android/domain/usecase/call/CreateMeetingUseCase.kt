package mega.privacy.android.domain.usecase.call

import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Create Meeting
 */
class CreateMeetingUseCase @Inject constructor(
    private val callRepository: CallRepository,
) {
    /**
     * Invoke
     *
     * @param title  Meeting title
     * @param speakRequest  Speak request enable
     * @param waitingRoom   Waiting room enable
     * @param openInvite    Open invite enable
     * @return       [ChatRequest]
     */
    suspend operator fun invoke(
        title: String,
        speakRequest: Boolean,
        waitingRoom: Boolean,
        openInvite: Boolean,
    ): ChatRequest = callRepository.createMeeting(
        title = title,
        speakRequest = speakRequest,
        waitingRoom = waitingRoom,
        openInvite = openInvite
    )
}