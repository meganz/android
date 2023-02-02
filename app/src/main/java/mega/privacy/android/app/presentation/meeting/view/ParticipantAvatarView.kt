package mega.privacy.android.app.presentation.meeting.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import coil.compose.rememberAsyncImagePainter
import mega.privacy.android.app.presentation.extensions.getAvatarFirstLetter
import mega.privacy.android.domain.entity.chat.ChatParticipant

/**
 * Avatar of a meeting with one participant view
 *
 * @param participant [ChatParticipant]
 */
@Composable
fun ParticipantAvatarView(participant: ChatParticipant) {
    if (participant.data.avatarUri == null) {
        DefaultMeetingAvatarView(
            title = participant.getAvatarFirstLetter(),
            colorBackground = Color(participant.defaultAvatarColor)
        )
    } else {
        Image(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape),
            painter = rememberAsyncImagePainter(model = participant.data.avatarUri),
            contentDescription = "User avatar"
        )
    }
}