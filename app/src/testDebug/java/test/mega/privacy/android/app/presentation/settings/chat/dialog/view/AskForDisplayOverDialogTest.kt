package test.mega.privacy.android.app.presentation.settings.chat.dialog.view

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.chat.dialog.view.AskForDisplayOverDialog
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import test.mega.privacy.android.app.fromId

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AskForDisplayOverDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun test_that_selecting_allow_calls_the_correct_function() {
        val onAllow = mock<() -> Unit>()
        composeTestRule.setContent {
            AskForDisplayOverDialog(show = true, onNotNow = {}, onAllow = onAllow)
        }

        composeTestRule.onNodeWithText(
            fromId(R.string.general_allow),
            ignoreCase = true
        ).performClick()

        verify(onAllow).invoke()
    }

    @Test
    fun test_that_selecting_not_now_calls_the_correct_function() {
        val onNotAllow = mock<() -> Unit>()
        composeTestRule.setContent {
            AskForDisplayOverDialog(show = true, onNotNow = onNotAllow, onAllow = {})
        }

        composeTestRule.onNodeWithText(
            fromId(R.string.verify_account_not_now_button),
            ignoreCase = true
        ).performClick()

        verify(onNotAllow).invoke()
    }
}