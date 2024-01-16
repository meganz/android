package mega.privacy.android.core.ui.controls.chat.messages

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class VoiceClipMessageViewTest {
    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that duration shows correctly`() {
        val duration = "00:49"
        initComposeRuleContent(
            progress = null,
            isPlaying = false,
            onPlayClicked = {},
        )
        composeRule.onNodeWithText(duration).assertExists()
    }

    @Test
    fun `test that progress indicator is visible when it is loading`() {
        initComposeRuleContent(
            progress = 50,
            isPlaying = false,
            onPlayClicked = {},
        )
        composeRule.onNodeWithTag(VOICE_CLIP_MESSAGE_VIEW_LOAD_PROGRESS_INDICATOR_TEST_TAG)
            .assertExists()
    }

    @Test
    fun `test that loading progress indicator does not exist when it is not loading`() {
        initComposeRuleContent(
            progress = null,
            isPlaying = false,
            onPlayClicked = {},
        )
        composeRule.onNodeWithTag(VOICE_CLIP_MESSAGE_VIEW_LOAD_PROGRESS_INDICATOR_TEST_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun `test that play button click is handled correct`() {
        val onClickMock = mock<() -> Unit>()
        initComposeRuleContent(
            progress = null,
            isPlaying = false,
            onPlayClicked = onClickMock,
        )
        composeRule.onNodeWithTag(VOICE_CLIP_MESSAGE_VIEW_PLAY_BUTTON_TEST_TAG).performClick()
        verify(onClickMock).invoke()
    }


    private fun initComposeRuleContent(
        progress: Int?,
        isPlaying: Boolean,
        onPlayClicked: () -> Unit,
    ) {
        composeRule.setContent {
            CoreVoiceClipMessageView(
                isMe = true,
                timestamp = "00:49",
                modifier = Modifier,
                loadProgress = progress,
                isPlaying = isPlaying,
                onPlayClicked = onPlayClicked,
            )
        }
    }
}
