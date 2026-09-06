package mega.privacy.android.feature.mediaplayer.presentation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.mediaplayer.data.AudioPlayerError
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@Config(sdk = [34])
@RunWith(AndroidJUnit4::class)
class AudioPlayerErrorDialogTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun setDialog(error: AudioPlayerError, onDismiss: () -> Unit = {}) {
        composeTestRule.setContent {
            AudioPlayerErrorDialog(error = error, onDismiss = onDismiss)
        }
    }

    private fun string(resId: Int): String = composeTestRule.activity.getString(resId)

    @Test
    fun `test that RetryExhausted with network shows the generic open-failure message`() {
        setDialog(AudioPlayerError.RetryExhausted(isNetworkAvailable = true))

        composeTestRule
            .onNodeWithText(string(sharedR.string.unable_to_open_selected_file_message))
            .assertIsDisplayed()
    }

    @Test
    fun `test that RetryExhausted without network shows the no-network message`() {
        setDialog(AudioPlayerError.RetryExhausted(isNetworkAvailable = false))

        composeTestRule
            .onNodeWithText(string(sharedR.string.pdf_viewer_error_no_network))
            .assertIsDisplayed()
    }

    @Test
    fun `test that NodeBlocked shows the taken-down message`() {
        setDialog(AudioPlayerError.NodeBlocked)

        composeTestRule
            .onNodeWithText(string(sharedR.string.file_info_taken_down_file_warning))
            .assertIsDisplayed()
    }

    @Test
    fun `test that QueueBuildFailed without network shows the no-network message`() {
        setDialog(AudioPlayerError.QueueBuildFailed(isNetworkAvailable = false))

        composeTestRule
            .onNodeWithText(string(sharedR.string.pdf_viewer_error_no_network))
            .assertIsDisplayed()
    }

    @Test
    fun `test that tapping OK invokes onDismiss`() {
        var dismissed = false
        setDialog(AudioPlayerError.QueueBuildFailed(isNetworkAvailable = true)) {
            dismissed = true
        }

        composeTestRule.onNodeWithText(string(sharedR.string.general_ok_only)).performClick()

        assertThat(dismissed).isTrue()
    }
}
