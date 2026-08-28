package mega.privacy.android.app.appstate.global.call

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.feature.chat.components.RETURN_TO_CALL_BANNER_CHRONOMETER_TAG
import mega.privacy.android.feature.chat.components.RETURN_TO_CALL_BANNER_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class OngoingCallBannerContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that banner is displayed when a call is in progress`() {
        setContent(uiStateWithCall(initialTimestamp = null))

        composeRule.onNodeWithTag(RETURN_TO_CALL_BANNER_TAG, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `test that banner is not displayed when there is no call`() {
        setContent(OngoingCallBannerUiState(currentCall = null, themeMode = ThemeMode.System))

        composeRule.onNodeWithTag(RETURN_TO_CALL_BANNER_TAG, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `test that chronometer is displayed when the call has a start timestamp`() {
        // The banner starts a 1s ticker while a timestamp is present; pin the clock so
        // waitForIdle does not spin on the never-ending delay loop.
        composeRule.mainClock.autoAdvance = false
        setContent(uiStateWithCall(initialTimestamp = Instant.now().epochSecond - 65))

        composeRule.onNodeWithTag(RETURN_TO_CALL_BANNER_CHRONOMETER_TAG, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `test that chronometer is not displayed when the call has no start timestamp`() {
        setContent(uiStateWithCall(initialTimestamp = null))

        composeRule.onNodeWithTag(RETURN_TO_CALL_BANNER_CHRONOMETER_TAG, useUnmergedTree = true).assertDoesNotExist()
    }

    private fun uiStateWithCall(initialTimestamp: Long?) = OngoingCallBannerUiState(
        currentCall = ChatCall(chatId = 1L, callId = 2L, initialTimestamp = initialTimestamp),
        themeMode = ThemeMode.System,
    )

    private fun setContent(uiState: OngoingCallBannerUiState) {
        composeRule.setContent {
            AndroidThemeForPreviews {
                OngoingCallBannerContent(uiState = uiState)
            }
        }
    }
}
