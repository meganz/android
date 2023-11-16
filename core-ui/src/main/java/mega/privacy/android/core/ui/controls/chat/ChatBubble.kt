package mega.privacy.android.core.ui.controls.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme
import mega.privacy.android.core.ui.theme.tokens.Colors

@Composable
fun ChatBubble(
    isMe: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .background(
                color = if (isMe) Colors.Accent.n900 else MegaTheme.colors.background.surface2,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {

        CompositionLocalProvider(
            LocalContentColor provides if (isMe) MegaTheme.colors.text.onColor else MegaTheme.colors.text.primary,
            LocalTextStyle provides MaterialTheme.typography.subtitle2,
        ) {
            content()
        }
    }
}

@CombinedThemePreviews
@Composable
private fun MeChatBubblePreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ChatBubble(isMe = true) {
            Text(text = "Hello world!")
        }
    }
}

@CombinedThemePreviews
@Composable
private fun OtherChatBubblePreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ChatBubble(isMe = false) {
            Text(text = "Hello world!")
        }
    }
}