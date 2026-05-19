package mega.privacy.android.app.presentation.meeting.mapper

import androidx.compose.ui.graphics.Color
import mega.privacy.android.app.presentation.extensions.getAvatarFirstLetter
import mega.privacy.android.app.presentation.meeting.model.ChatParticipantUiState
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.shared.contact.mapper.ContactItemStatusMapper
import mega.privacy.android.shared.contact.model.AvatarData
import mega.privacy.android.shared.contact.model.ContactItemUiState
import java.io.File
import javax.inject.Inject

/**
 * Maps a domain [ChatParticipant] to the presentational
 * [ChatParticipantUiState] consumed by the Chat Info screen.
 */
class ChatParticipantUiStateMapper @Inject constructor(
    private val contactItemStatusMapper: ContactItemStatusMapper,
) {
    /**
     * @param participant Domain participant.
     */
    operator fun invoke(participant: ChatParticipant): ChatParticipantUiState =
        ChatParticipantUiState(
            contactItem = ContactItemUiState(
                handle = participant.handle,
                displayName = resolveDisplayName(participant),
                status = contactItemStatusMapper(participant.status),
                lastSeen = participant.lastSeen,
                avatar = resolveAvatar(participant),
                isVerified = participant.areCredentialsVerified,
            ),
            isMe = participant.isMe,
            privilege = participant.privilege,
            email = participant.email,
            avatarUpdateTimestamp = participant.avatarUpdateTimestamp,
        )

    private fun resolveDisplayName(participant: ChatParticipant): String =
        participant.data.alias
            ?: participant.data.fullName
            ?: participant.email
            ?: ""

    private fun resolveAvatar(participant: ChatParticipant): AvatarData {
        val avatarUri = participant.data.avatarUri
        return if (avatarUri != null) {
            AvatarData.Image(file = File(avatarUri))
        } else {
            AvatarData.Initials(
                initials = participant.getAvatarFirstLetter(),
                avatarColor = Color(participant.defaultAvatarColor),
            )
        }
    }
}
