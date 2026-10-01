package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.entity.chat.ChatScheduledFlags
import mega.privacy.android.domain.entity.chat.ChatScheduledRules
import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Create Chatroom and scheduled meeting Use Case
 */
class CreateChatroomAndSchedMeetingUseCase @Inject constructor(
    private val callRepository: CallRepository,
) {
    /**
     * Invoke
     *
     * @param peerList List of peers
     * @param isMeeting True to create a meeting room, otherwise false
     * @param publicChat True to create a public chat, otherwise false
     * @param title Title of scheduled meeting
     * @param speakRequest True to set that during calls non moderator users, must request permission to speak
     * @param waitingRoom True to set that during calls, non moderator members will be placed into a waiting room.
     * A moderator user must grant each user access to the call.
     * @param openInvite to set that users with MegaChatRoom::PRIV_STANDARD privilege, can invite other users into the chat
     * @param timezone Timezone where we want to schedule the meeting
     * @param startDate start date time of the meeting with the format (unix timestamp UTC)
     * @param endDate end date time of the meeting with the format (unix timestamp UTC)
     * @param description Description of scheduled meeting
     * Note that description is a mandatory field, so in case you want to set an empty description, please provide an empty string with Null-terminated character at the end
     * @param flags [ChatScheduledFlags]
     * @param rules [ChatScheduledRules]
     * @param attributes - not supported yet
     * @return                  [ChatRequest]
     */
    suspend operator fun invoke(
        peerList: List<Long>,
        isMeeting: Boolean,
        publicChat: Boolean,
        title: String,
        speakRequest: Boolean,
        waitingRoom: Boolean,
        openInvite: Boolean,
        timezone: String,
        startDate: Long,
        endDate: Long,
        description: String,
        flags: ChatScheduledFlags?,
        rules: ChatScheduledRules?,
        attributes: String?,
    ): ChatRequest =
        callRepository.createChatroomAndSchedMeeting(
            peerList,
            isMeeting,
            publicChat,
            title,
            speakRequest,
            waitingRoom,
            openInvite,
            timezone,
            startDate,
            endDate,
            description,
            flags,
            rules,
            attributes
        )
}