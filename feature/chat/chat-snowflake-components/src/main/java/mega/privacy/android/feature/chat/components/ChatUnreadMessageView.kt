package mega.privacy.android.feature.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens

/**
 * Full-width separator that marks the first unread message in a chat room.
 *
 * @param content Formatted unread message text, for example "5 unread messages".
 * @param modifier [Modifier]
 */
@Composable
fun ChatUnreadMessageView(
    content: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(color = DSTokens.colors.background.surface2)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        MegaText(
            text = content,
            textColor = TextColor.Primary,
            style = AppTheme.typography.bodyLarge,
            modifier = Modifier.testTag(CHAT_UNREAD_MESSAGE_VIEW_TAG),
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ChatUnreadMessageViewPreview() {
    AndroidThemeForPreviews {
        ChatUnreadMessageView(content = "5 unread messages")
    }
}

/**
 * Test tag for the unread message separator text.
 */
const val CHAT_UNREAD_MESSAGE_VIEW_TAG = "chat_unread_message_view:text"
