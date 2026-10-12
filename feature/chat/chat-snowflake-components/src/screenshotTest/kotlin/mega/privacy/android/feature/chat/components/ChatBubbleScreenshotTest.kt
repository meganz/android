package mega.privacy.android.feature.chat.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.TextColor

/**
 * Screenshot tests for [ChatBubble], covering own and other messages with and without sub content.
 */
class ChatBubbleScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChatBubbleMine() {
        AndroidThemeForPreviews {
            SampleBubble(isMine = true, withSubContent = false)
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChatBubbleOther() {
        AndroidThemeForPreviews {
            SampleBubble(isMine = false, withSubContent = false)
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChatBubbleMineWithSubContent() {
        AndroidThemeForPreviews {
            SampleBubble(isMine = true, withSubContent = true)
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChatBubbleOtherWithSubContent() {
        AndroidThemeForPreviews {
            SampleBubble(isMine = false, withSubContent = true)
        }
    }

    @Composable
    private fun SampleBubble(isMine: Boolean, withSubContent: Boolean) {
        ChatBubble(
            isMine = isMine,
            subContent = if (withSubContent) {
                {
                    MegaText(
                        text = "mega.io - Secure cloud storage",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            } else {
                null
            },
        ) {
            MegaText(
                text = "Hello world!",
                textColor = if (isMine) TextColor.Inverse else TextColor.Primary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}
