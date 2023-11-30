package mega.privacy.android.app.presentation.meeting.chat.view.message

import android.text.format.DateFormat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.app.presentation.meeting.view.getRecurringMeetingDateTime
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.controls.chat.messages.FirstMessageHeaderParagraph
import mega.privacy.android.core.ui.controls.chat.messages.FirstMessageHeaderSubtitleWithIcon
import mega.privacy.android.core.ui.controls.chat.messages.FirstMessageHeaderTitle
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.contacts.UserChatStatus

@Composable
internal fun FirstMessageHeader(uiState: ChatUiState) {
    val context = LocalContext.current
    val is24HourFormat = remember { DateFormat.is24HourFormat(context) }
    Column(
        modifier = Modifier.padding(start = 72.dp, top = 40.dp, end = 24.dp),
    ) {
        uiState.title?.let { title ->
            val subtitle = uiState.scheduledMeeting?.let { scheduledMeeting ->
                getRecurringMeetingDateTime(
                    scheduledMeeting = scheduledMeeting,
                    is24HourFormat = is24HourFormat,
                ).text
            }
            FirstMessageHeaderTitle(
                title = title,
                subtitle = subtitle,
                modifier = Modifier.padding(bottom = 24.dp),
            )
        }

        FirstMessageHeaderParagraph(
            paragraph = stringResource(id = R.string.chat_chatroom_first_message_header_mega_info_text),
            modifier = Modifier.padding(bottom = 24.dp),
        )
        FirstMessageHeaderSubtitleWithIcon(
            subtitle = stringResource(id = R.string.title_mega_confidentiality_empty_screen),
            iconRes = R.drawable.ic_lock
        )
        FirstMessageHeaderParagraph(
            paragraph = stringResource(id = R.string.mega_confidentiality_empty_screen),
            modifier = Modifier.padding(bottom = 24.dp),
        )
        FirstMessageHeaderSubtitleWithIcon(
            subtitle = stringResource(id = R.string.title_mega_authenticity_empty_screen),
            iconRes = mega.privacy.android.core.R.drawable.ic_check_circle
        )
        FirstMessageHeaderParagraph(
            paragraph = stringResource(id = R.string.chat_chatroom_first_message_header_authenticity_info_text)
        )
    }
}

@CombinedThemePreviews
@Composable
private fun FirstMessageHeaderPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        FirstMessageHeader(
            uiState = ChatUiState(
                title = "My Name",
                userChatStatus = UserChatStatus.Away,
                isChatNotificationMute = true,
                isPrivateChat = true,
                myPermission = ChatRoomPermission.Standard,
            )
        )
    }
}