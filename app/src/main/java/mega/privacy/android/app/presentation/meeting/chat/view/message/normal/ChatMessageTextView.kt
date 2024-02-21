package mega.privacy.android.app.presentation.meeting.chat.view.message.normal

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.app.presentation.meeting.chat.model.MessageListViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.message.getMessageText
import mega.privacy.android.core.ui.controls.chat.messages.ChatBubble
import mega.privacy.android.core.ui.controls.text.megaSpanStyle
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.tokens.TextColor
import mega.privacy.android.domain.entity.chat.messages.normal.TextMessage
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * Text view for chat message
 *
 * @param message Text message
 * @param modifier Modifier
 */
@Composable
fun ChatMessageTextView(
    message: TextMessage,
    isEdited: Boolean,
    modifier: Modifier = Modifier,
    viewModel: ChatMessageTextViewModel = hiltViewModel(),
) {
    val richLinkConfig by viewModel.richLinkConfig.collectAsStateWithLifecycle()
    val parentViewModel = hiltViewModel<MessageListViewModel>()
    val lastedMessageId by parentViewModel.latestMessageId
    val askedEnableRichLink by parentViewModel.askedEnableRichLink

    ChatMessageTextView(
        text = message.content,
        isMe = message.isMine,
        counterNotNowRichLinkWarning = richLinkConfig.counterNotNowRichLinkWarning,
        shouldShowWarning = lastedMessageId == message.msgId
                && message.isMine
                && richLinkConfig.isShowRichLinkWarning
                && message.hasOtherLink
                && !askedEnableRichLink,
        modifier = modifier,
        onAskedEnableRichLink = parentViewModel::onAskedEnableRichLink,
        enableRichLinkPreview = viewModel::enableRichLinkPreview,
        setRichLinkWarningCounter = viewModel::setRichLinkWarningCounter,
        isEdited = isEdited,
    )
}

/**
 * Text view for chat message
 */
@Composable
fun ChatMessageTextView(
    text: String,
    isMe: Boolean,
    shouldShowWarning: Boolean,
    counterNotNowRichLinkWarning: Int,
    modifier: Modifier = Modifier,
    onAskedEnableRichLink: () -> Unit = {},
    enableRichLinkPreview: (Boolean) -> Unit = {},
    setRichLinkWarningCounter: (Int) -> Unit = {},
    isEdited: Boolean,
) {

    ChatBubble(modifier = modifier, isMe = isMe, subContent = {
        if (shouldShowWarning) {
            EnableRichLinkView(
                alwaysAllowClick = {
                    enableRichLinkPreview(true)
                    onAskedEnableRichLink()
                },
                notNowClick = {
                    setRichLinkWarningCounter(
                        counterNotNowRichLinkWarning
                            .inc()
                            .coerceAtLeast(1)
                    )
                    onAskedEnableRichLink()
                },
                neverClick = {
                    enableRichLinkPreview(false)
                    onAskedEnableRichLink()
                },
                denyNeverClick = {
                    setRichLinkWarningCounter(
                        counterNotNowRichLinkWarning
                            .inc()
                            .coerceAtLeast(1)
                    )
                    onAskedEnableRichLink()
                },
                isShowNeverButton = counterNotNowRichLinkWarning >= 3,
            )
        }
    }) {
        Text(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            text = getMessageText(
                message = text,
                isEdited = isEdited,
                spansStyle = megaSpanStyle(
                    fontStyle = FontStyle.Italic,
                    color = TextColor.Disabled
                )
            ),
        )
    }
}

@CombinedThemePreviews
@Composable
private fun MeChatMessageTextPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        ChatMessageTextView(
            text = "Hello World",
            isMe = true,
            shouldShowWarning = true,
            counterNotNowRichLinkWarning = 3,
            isEdited = false,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun OtherChatMessageTextPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        ChatMessageTextView(
            text = "Hello World",
            isMe = true,
            shouldShowWarning = true,
            counterNotNowRichLinkWarning = 3,
            isEdited = false,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun EditedChatMessageTextPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        ChatMessageTextView(
            text = "Hello World",
            isMe = true,
            shouldShowWarning = true,
            counterNotNowRichLinkWarning = 3,
            isEdited = true,
        )
    }
}