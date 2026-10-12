package mega.privacy.android.feature.chat.components

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews

/**
 * Screenshot tests for [ChatUnreadMessageView].
 */
class ChatUnreadMessageViewScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChatUnreadMessageViewDefault() {
        AndroidThemeForPreviews {
            ChatUnreadMessageView(content = "5 unread messages")
        }
    }
}
