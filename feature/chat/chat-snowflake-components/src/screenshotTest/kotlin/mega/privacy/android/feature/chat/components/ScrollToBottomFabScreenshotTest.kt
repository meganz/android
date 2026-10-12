package mega.privacy.android.feature.chat.components

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews

/**
 * Screenshot tests for [ScrollToBottomFab], covering no badge, a one-digit count, a two-digit
 * count and a count above the cap.
 */
class ScrollToBottomFabScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ScrollToBottomFabWithoutUnread() {
        AndroidThemeForPreviews {
            ScrollToBottomFab(unreadCount = 0, onClick = {})
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ScrollToBottomFabWithOneUnread() {
        AndroidThemeForPreviews {
            ScrollToBottomFab(unreadCount = 1, onClick = {})
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ScrollToBottomFabWithMaxUnread() {
        AndroidThemeForPreviews {
            ScrollToBottomFab(unreadCount = 99, onClick = {})
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ScrollToBottomFabAboveMaxUnread() {
        AndroidThemeForPreviews {
            ScrollToBottomFab(unreadCount = 199, onClick = {})
        }
    }
}
