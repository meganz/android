package mega.privacy.android.feature.contact.info.view

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContactInfoActionBarTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val invocations = mutableListOf<String>()

    @Test
    fun `test that tapping send message invokes onSendMessageClick`() {
        setActionBar(callButtonsEnabled = true)

        actionButton(CONTACT_INFO_ACTION_MESSAGE_TAG).performClick()

        assertThat(invocations).containsExactly("onSendMessageClick")
    }

    @Test
    fun `test that tapping audio call invokes onStartAudioCallClick`() {
        setActionBar(callButtonsEnabled = true)

        actionButton(CONTACT_INFO_ACTION_AUDIO_CALL_TAG).performClick()

        assertThat(invocations).containsExactly("onStartAudioCallClick")
    }

    @Test
    fun `test that tapping video call invokes onStartVideoCallClick`() {
        setActionBar(callButtonsEnabled = true)

        actionButton(CONTACT_INFO_ACTION_VIDEO_CALL_TAG).performClick()

        assertThat(invocations).containsExactly("onStartVideoCallClick")
    }

    @Test
    fun `test that the call buttons are disabled when callButtonsEnabled is false`() {
        setActionBar(callButtonsEnabled = false)

        actionButton(CONTACT_INFO_ACTION_AUDIO_CALL_TAG).assertIsNotEnabled()
        actionButton(CONTACT_INFO_ACTION_VIDEO_CALL_TAG).assertIsNotEnabled()
    }

    @Test
    fun `test that tapping the call buttons invokes nothing when callButtonsEnabled is false`() {
        setActionBar(callButtonsEnabled = false)

        actionButton(CONTACT_INFO_ACTION_AUDIO_CALL_TAG).performClick()
        actionButton(CONTACT_INFO_ACTION_VIDEO_CALL_TAG).performClick()

        assertThat(invocations).isEmpty()
    }

    @Test
    fun `test that send message stays enabled when callButtonsEnabled is false`() {
        setActionBar(callButtonsEnabled = false)

        actionButton(CONTACT_INFO_ACTION_MESSAGE_TAG).assertIsEnabled().performClick()

        assertThat(invocations).containsExactly("onSendMessageClick")
    }

    private fun actionButton(tag: String) =
        composeTestRule.onNodeWithTag(tag).onChildren().filterToOne(hasClickAction())

    private fun setActionBar(callButtonsEnabled: Boolean) {
        composeTestRule.setContent {
            ContactInfoActionBar(
                callButtonsEnabled = callButtonsEnabled,
                onSendMessageClick = { invocations.add("onSendMessageClick") },
                onStartAudioCallClick = { invocations.add("onStartAudioCallClick") },
                onStartVideoCallClick = { invocations.add("onStartVideoCallClick") },
            )
        }
    }
}
