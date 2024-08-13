package test.mega.privacy.android.app.presentation.meeting

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.main.megachat.AppRTCAudioManager
import mega.privacy.android.app.meeting.fragments.MeetingActionButtonsTestTags
import mega.privacy.android.app.meeting.fragments.MeetingsActionButtons
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import test.mega.privacy.android.app.onNodeWithText
import mega.privacy.android.domain.entity.call.AudioDevice


@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w720dp-h1280dp-xhdpi")
class MeetingsActionButtonsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that microphone button is displayed`() {
        setupRule()
        with(composeTestRule) {
            onNodeWithTag(MeetingActionButtonsTestTags.MIC_BUTTON)
                .assertIsDisplayed()
            onNodeWithText(R.string.general_mic)
        }
    }

    @Test
    fun `test that camera button is displayed`() {
        setupRule()
        with(composeTestRule) {
            onNodeWithTag(MeetingActionButtonsTestTags.CAMERA_BUTTON)
                .assertIsDisplayed()
            onNodeWithText(R.string.general_camera)
        }
    }

    @Test
    fun `test that speaker button is displayed`() {
        setupRule()
        with(composeTestRule) {
            onNodeWithTag(MeetingActionButtonsTestTags.SPEAKER_BUTTON)
                .assertIsDisplayed()
            onNodeWithText(R.string.general_speaker)
        }
    }

    @Test
    fun `test that more button is displayed`() {
        setupRule()
        with(composeTestRule) {
            onNodeWithTag(MeetingActionButtonsTestTags.MORE_BUTTON)
                .assertIsDisplayed()
            onNodeWithText(R.string.meetings_more_call_option_button)
        }
    }

    @Test
    fun `test that end button is displayed`() {
        setupRule()
        with(composeTestRule) {
            onNodeWithTag(MeetingActionButtonsTestTags.END_CALL_BUTTON)
                .assertIsDisplayed()
            onNodeWithText(R.string.meeting_end)
        }
    }

    private fun setupRule() {
        composeTestRule.setContent {
            MeetingsActionButtons(
                onMicClicked = {},
                onCamClicked = {},
                onSpeakerClicked = {},
                onMoreClicked = {},
                onEndClicked = {},
                micEnabled = true,
                cameraEnabled = true,
                speakerEnabled = true,
                moreEnabled = true,
                showMicWarning = false,
                buttonsEnabled = true,
                showCameraWarning = false,
                backgroundTintAlpha = 1.0F,
                isRaiseHandToolTipShown = false,
                onRaiseToRandTooltipDismissed = {},
                tooltipKey = 0,
                currentAudioDevice = AudioDevice.None
            )
        }
    }
}
