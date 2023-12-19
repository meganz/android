package mega.privacy.android.app.presentation.meeting.chat.view.message.meetingupdate

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.view.message.management.ChatManagementMessageView
import mega.privacy.android.app.presentation.meeting.chat.view.message.management.ManagementMessageViewModel
import mega.privacy.android.core.ui.model.MegaSpanStyle
import mega.privacy.android.core.ui.model.SpanIndicator
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.tokens.TextColor
import mega.privacy.android.domain.entity.chat.messages.management.ScheduledMeetingUpdatedMessage
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * Scheduled meeting update message view
 *
 * @param message The message
 * @param modifier Modifier
 * @param viewModel The view model
 */
@Composable
fun ScheduledMeetingUpdateMessageView(
    message: ScheduledMeetingUpdatedMessage,
    modifier: Modifier = Modifier,
    viewModel: ManagementMessageViewModel = hiltViewModel(),
) {
    val context: Context = LocalContext.current
    var ownerActionFullName by remember {
        mutableStateOf(context.getString(R.string.unknown_name_label))
    }

    LaunchedEffect(Unit) {
        if (message.isMine) {
            viewModel.getMyFullName()
        } else {
            viewModel.getParticipantFullName(message.userHandle)
        }?.let { ownerActionFullName = it }
    }

    ScheduledMeetingUpdateMessageView(
        ownerActionFullName = ownerActionFullName,
        modifier = modifier
    )
}

/**
 * Scheduled meeting update message view
 *
 * @param ownerActionFullName
 * @param modifier
 */
@Composable
fun ScheduledMeetingUpdateMessageView(
    ownerActionFullName: String,
    modifier: Modifier = Modifier,
) {
    ChatManagementMessageView(
        text = stringResource(
            R.string.chat_chat_room_message_updated_scheduled_meeting,
            ownerActionFullName
        ),
        modifier = modifier,
        styles = mapOf(
            SpanIndicator('A') to MegaSpanStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                ),
                color = TextColor.Primary
            ),
            SpanIndicator('B') to MegaSpanStyle(
                SpanStyle(),
                color = TextColor.Secondary
            ),
            SpanIndicator('C') to MegaSpanStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                ),
                color = TextColor.Primary
            ),
        )
    )
}

@CombinedThemePreviews
@Composable
private fun ScheduledMeetingUpdatedMessageViewPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        ScheduledMeetingUpdateMessageView(
            ownerActionFullName = "Owner",
        )
    }
}