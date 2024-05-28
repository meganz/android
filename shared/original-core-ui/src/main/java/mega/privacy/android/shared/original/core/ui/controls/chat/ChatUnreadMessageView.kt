package mega.privacy.android.shared.original.core.ui.controls.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor

/**
 * Chat unread message view
 *
 * @param content Unread count content
 */
@Composable
fun ChatUnreadMessageView(
    content: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(color = MegaOriginalTheme.colors.background.surface2)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        MegaText(
            text = content,
            textColor = TextColor.Primary,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ChatUnreadMessageViewPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        ChatUnreadMessageView("5 unread messages")
    }
}
