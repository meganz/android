package test.mega.privacy.android.app.main.view

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.main.view.OngoingCallBannerContent
import mega.privacy.android.app.main.view.OngoingCallUiState
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.call.ChatCall
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import test.mega.privacy.android.app.onNodeWithText

@RunWith(AndroidJUnit4::class)
class OngoingCallBannerTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `test that the banner is shown when the call is in progress`() {
        composeRule.setContent {
            OngoingCallBannerContent(
                uiState = OngoingCallUiState(
                    currentCall = ChatCall(chatId = 1L, callId = 1L),
                    isShown = true,
                    themeMode = ThemeMode.System
                ),
                onShow = {}
            )
        }

        composeRule.onNodeWithText(R.string.call_in_progress_layout).assertExists()
    }
}