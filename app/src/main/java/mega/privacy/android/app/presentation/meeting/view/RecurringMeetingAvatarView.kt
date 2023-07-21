package mega.privacy.android.app.presentation.meeting.view

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.chat.list.view.ChatAvatarView
import mega.privacy.android.app.presentation.extensions.getAvatarFirstLetter
import mega.privacy.android.app.presentation.meeting.model.RecurringMeetingInfoState

/**
 * Create meeting avatar view
 *
 * @param state     [RecurringMeetingInfoState]
 */
@Composable
fun RecurringMeetingAvatarView(state: RecurringMeetingInfoState) {
    if (state.isEmptyMeeting()) {
        state.schedTitle?.let {
            ChatAvatarView(
                avatarUri = null,
                avatarPlaceholder = it,
                avatarColor = null,
                modifier = Modifier.border(1.dp, Color.White, CircleShape)
            )
        }
    } else if (state.isSingleMeeting()) {
        state.firstParticipant?.let { participant ->
            ChatAvatarView(
                avatarUri = participant.data.avatarUri,
                avatarPlaceholder = participant.getAvatarFirstLetter(),
                avatarColor = participant.defaultAvatarColor,
                avatarTimestamp = participant.avatarUpdateTimestamp,
                modifier = Modifier.border(1.dp, Color.White, CircleShape),
            )
        }
    } else if (state.firstParticipant != null && state.secondParticipant != null) {
        Box(
            Modifier.fillMaxSize()
        ) {
            ChatAvatarView(
                avatarUri = state.secondParticipant.data.avatarUri,
                avatarPlaceholder = state.secondParticipant.getAvatarFirstLetter(),
                avatarColor = state.secondParticipant.defaultAvatarColor,
                avatarTimestamp = state.secondParticipant.avatarUpdateTimestamp,
                modifier = Modifier
                    .size(26.dp)
                    .align(Alignment.BottomEnd)
                    .border(1.dp, Color.White, CircleShape)
            )
            ChatAvatarView(
                avatarUri = state.firstParticipant.data.avatarUri,
                avatarPlaceholder = state.firstParticipant.getAvatarFirstLetter(),
                avatarColor = state.firstParticipant.defaultAvatarColor,
                avatarTimestamp = state.firstParticipant.avatarUpdateTimestamp,
                modifier = Modifier
                    .size(26.dp)
                    .align(Alignment.TopStart)
                    .border(1.dp, Color.White, CircleShape)
            )
        }
    }
}