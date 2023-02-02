package mega.privacy.android.app.presentation.meeting.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import mega.privacy.android.app.presentation.extensions.getAvatarFirstLetter
import mega.privacy.android.domain.entity.chat.ChatParticipant


/**
 * Avatar of a meeting with several participants view
 *
 * @param firstParticipant      [ChatParticipant]
 * @param secondParticipant     [ChatParticipant]
 */
@Composable
fun ParticipantsAvatarView(
    firstParticipant: ChatParticipant,
    secondParticipant: ChatParticipant,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .border(width = 1.dp, color = Color.White, shape = CircleShape)
                .clip(CircleShape)
                .align(Alignment.BottomEnd)
                .background(
                    color = Color(secondParticipant.defaultAvatarColor),
                    shape = CircleShape
                )
        ) {
            if (secondParticipant.data.avatarUri == null) {
                Text(
                    text = secondParticipant.getAvatarFirstLetter(),
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    style = MaterialTheme.typography.subtitle1
                )
            } else {
                Image(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape),
                    painter = rememberAsyncImagePainter(model = secondParticipant.data.avatarUri),
                    contentDescription = "User avatar"
                )
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .border(width = 1.dp, color = Color.White, shape = CircleShape)
                .clip(CircleShape)
                .align(Alignment.TopStart)
                .background(
                    color = Color(firstParticipant.defaultAvatarColor),
                    shape = CircleShape
                )
        ) {
            if (firstParticipant.data.avatarUri == null) {
                Text(
                    text = firstParticipant.getAvatarFirstLetter(),
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    style = MaterialTheme.typography.subtitle1
                )
            } else {
                Image(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape),
                    painter = rememberAsyncImagePainter(model = firstParticipant.data.avatarUri),
                    contentDescription = "User avatar"
                )
            }
        }
    }
}