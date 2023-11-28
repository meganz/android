package mega.privacy.android.app.presentation.meeting.chat.view.message

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.controls.chat.ChatBubble
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme

/**
 * Text view for chat message
 *
 * @param text the text to show
 * @param modifier Modifier
 */
@Composable
fun ChatMessageTextView(
    text: String,
    isMe: Boolean,
    modifier: Modifier = Modifier,
) {
    ChatBubble(modifier = modifier, isMe = isMe) {
        Text(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            text = text,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun MeChatMessageTextPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ChatMessageTextView(isMe = true, text = "Hello World")
    }
}

@CombinedThemePreviews
@Composable
private fun OtherChatMessageTextPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ChatMessageTextView(isMe = false, text = "Hello World")
    }
}