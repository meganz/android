package mega.privacy.android.app.presentation.meeting.chat.view.message.title

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
import mega.privacy.android.app.presentation.meeting.chat.view.message.ChatManagementMessageView
import mega.privacy.android.app.presentation.meeting.chat.view.message.ManagementMessageViewModel
import mega.privacy.android.core.ui.model.MegaSpanStyle
import mega.privacy.android.core.ui.model.SpanIndicator
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.tokens.TextColor
import mega.privacy.android.domain.entity.chat.messages.management.TitleChangeMessage
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * Title change message view
 *
 * @param message The message
 * @param modifier Modifier
 * @param viewModel The view model
 */
@Composable
fun TitleChangeMessageView(
    message: TitleChangeMessage,
    modifier: Modifier = Modifier,
    viewModel: ManagementMessageViewModel = hiltViewModel(),
) {
    var ownerActionFullName by remember { mutableStateOf("") }
    val context: Context = LocalContext.current
    LaunchedEffect(Unit) {
        ownerActionFullName = if (message.isMine) {
            viewModel.getMyFullName()
        } else {
            viewModel.getParticipantFullName(message.userHandle)
        } ?: context.getString(R.string.unknown_name_label)
    }
    TitleChangeMessageView(
        message = message,
        ownerActionFullName = ownerActionFullName,
        modifier = modifier
    )
}

/**
 * Title change message view
 *
 * @param message
 * @param ownerActionFullName
 * @param modifier
 */
@Composable
fun TitleChangeMessageView(
    message: TitleChangeMessage,
    ownerActionFullName: String,
    modifier: Modifier = Modifier,
) {
    val titleChangeMessage =
        stringResource(id = R.string.change_title_messages, ownerActionFullName, message.content)
    ChatManagementMessageView(
        text = titleChangeMessage,
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
private fun TitleChangeMessageViewPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        TitleChangeMessageView(
            message = TitleChangeMessage(
                userHandle = 0,
                isMine = true,
                content = "New title",
                time = 0,
                msgId = 1
            ),
            ownerActionFullName = "My name"
        )
    }
}