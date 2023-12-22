package mega.privacy.android.app.presentation.meeting.chat.view.message

import android.text.format.DateFormat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.view.getRecurringMeetingDateTime
import mega.privacy.android.core.ui.controls.chat.messages.FirstMessageHeaderParagraph
import mega.privacy.android.core.ui.controls.chat.messages.FirstMessageHeaderSubtitleWithIcon
import mega.privacy.android.core.ui.controls.chat.messages.FirstMessageHeaderTitle
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.shared.theme.MegaAppTheme

@Composable
internal fun FirstMessageHeader(
    title: String?,
    scheduledMeeting: ChatScheduledMeeting?,
) {
    val context = LocalContext.current
    val is24HourFormat = remember { DateFormat.is24HourFormat(context) }
    Column(
        modifier = Modifier
            .padding(start = 72.dp, top = 40.dp, end = 24.dp)
            .testTag(TEST_TAG_FIRST_MESSAGE_HEADER),
    ) {
        title?.let {
            val subtitle = scheduledMeeting?.let { scheduledMeeting ->
                getRecurringMeetingDateTime(
                    scheduledMeeting = scheduledMeeting,
                    is24HourFormat = is24HourFormat,
                ).text
            }
            FirstMessageHeaderTitle(
                title = it,
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
            scheduledMeeting = null,
            title = "My name"
        )
    }
}

internal const val TEST_TAG_FIRST_MESSAGE_HEADER = "chat_view:first_message_header"