package mega.privacy.android.feature.chat.components

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews

/**
 * Screenshot tests for [DateHeader] and [TimeHeader], covering own and other alignment and the
 * header without a sender name.
 */
class ChatMessageHeadersScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun DateHeaderDefault() {
        AndroidThemeForPreviews {
            DateHeader(dateString = "Friday, Nov 14, 2023")
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun TimeHeaderMine() {
        AndroidThemeForPreviews {
            TimeHeader(timeString = "10:15", displayAsMine = true, userName = null)
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun TimeHeaderOtherWithUserName() {
        AndroidThemeForPreviews {
            TimeHeader(timeString = "10:15", displayAsMine = false, userName = "John Doe")
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun TimeHeaderOtherWithoutUserName() {
        AndroidThemeForPreviews {
            TimeHeader(timeString = "10:15", displayAsMine = false, userName = null)
        }
    }
}
