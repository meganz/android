package mega.privacy.android.app.presentation.meeting.mapper

import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.entity.meeting.CallParticipantData
import javax.inject.Inject

/**
 * Mapper to convert [Participant] to [ChatParticipant]
 */
class ChatParticipantMapper @Inject constructor() {
    internal operator fun invoke(
        participant: Participant,
        chatParticipant: ChatParticipant,
    ): ChatParticipant = ChatParticipant(
        handle = chatParticipant.handle,
        data = chatParticipant.data,
        email = chatParticipant.email,
        isMe = chatParticipant.isMe,
        privilege = chatParticipant.privilege,
        defaultAvatarColor = chatParticipant.defaultAvatarColor,
        areCredentialsVerified = chatParticipant.areCredentialsVerified,
        status = chatParticipant.status,
        lastSeen = chatParticipant.lastSeen,
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