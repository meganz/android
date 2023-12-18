package mega.privacy.android.app.presentation.meeting.chat.view.message.management

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
import mega.privacy.android.core.ui.model.MegaSpanStyle
import mega.privacy.android.core.ui.model.SpanIndicator
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.tokens.TextColor
import mega.privacy.android.domain.entity.chat.messages.management.ChatLinkCreatedMessage
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * Chat link created message view.
 */
@Composable
fun ChatLinkCreatedView(
    message: ChatLinkCreatedMessage,
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

    ChatLinkCreatedView(
        ownerActionFullName = ownerActionFullName,
        modifier = modifier
    )
}

/**
 * Chat link created message view.
 *
 * @param ownerActionFullName The owner action full name
 * @param modifier Modifier
 */
@Composable
internal fun ChatLinkCreatedView(
    ownerActionFullName: String,
    modifier: Modifier = Modifier,
) = ChatManagementMessageView(
    text = stringResource(id = R.string.message_created_chat_link, ownerActionFullName),
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
    )
)

@CombinedThemePreviews
@Composable
private fun ChatLinkCreatedPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        ChatLinkCreatedView(
            ownerActionFullName = "Name"
        )
    }
}