package mega.privacy.android.app.presentation.meeting.mapper

import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.entity.call.CallParticipantData
import javax.inject.Inject

/**
 * Mapper to convert [Participant] to [ChatParticipant]
 */
class ChatParticipantMapper @Inject constructor() {
    internal operator fun invoke(
        participant: Participant,
        chatParticipant: ChatParticipant,
        raisedHandAndOrder: Pair<Boolean, Int>? = null,
    ): ChatParticipant {
        return chatParticipant.copy(
            isRaisedHand = raisedHandAndOrder?.first ?: false,
            order = raisedHandAndOrder?.second ?: Int.MAX_VALUE,
            callParticipantData = CallParticipantData(
                clientId = participant.clientId,
                isAudioOn = participant.isAudioOn,
                isVideoOn = participant.isVideoOn,
                isContact = participant.isContact,
                isSpeaker = participant.isSpeaker,
                isGuest = participant.isGuest,
            ),
        )
    }
}
