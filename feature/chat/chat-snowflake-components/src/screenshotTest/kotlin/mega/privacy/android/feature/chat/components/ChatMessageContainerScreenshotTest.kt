package mega.privacy.android.feature.chat.components

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews

/**
 * Screenshot tests for [ChatMessageContainer], covering own and other messages, the forward
 * button, long text, the send error, the reactions slot and select mode.
 */
class ChatMessageContainerScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChatMessageContainerMine() = Sample(ChatMessageContainerPreviewParameter(isMine = true))

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChatMessageContainerOther() = Sample(ChatMessageContainerPreviewParameter(isMine = false))

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChatMessageContainerMineWithoutForward() = Sample(
        ChatMessageContainerPreviewParameter(isMine = true, showForward = false)
    )

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChatMessageContainerMineLongText() = Sample(
        ChatMessageContainerPreviewParameter(isMine = true, text = LONG_TEXT)
    )

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChatMessageContainerOtherLongText() = Sample(
        ChatMessageContainerPreviewParameter(isMine = false, text = LONG_TEXT)
    )

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChatMessageContainerMineSendError() = Sample(
        ChatMessageContainerPreviewParameter(isMine = true, hasSendError = true)
    )

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChatMessageContainerMineWithReactions() = Sample(
        ChatMessageContainerPreviewParameter(isMine = true, hasReactions = true)
    )

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChatMessageContainerOtherWithReactions() = Sample(
        ChatMessageContainerPreviewParameter(isMine = false, hasReactions = true)
    )

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChatMessageContainerMineSelected() = Sample(
        ChatMessageContainerPreviewParameter(isMine = true, isSelectMode = true, isSelected = true)
    )

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChatMessageContainerOtherNotSelected() = Sample(
        ChatMessageContainerPreviewParameter(isMine = false, isSelectMode = true)
    )

    @Composable
    private fun Sample(parameter: ChatMessageContainerPreviewParameter) {
        AndroidThemeForPreviews {
            ChatMessageContainerSample(parameter)
        }
    }

    private companion object {
        const val LONG_TEXT =
            "Krakatoa is a caldera in the Sunda Strait between the islands of Java and Sumatra in Indonesia"
    }
}
