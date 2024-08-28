package mega.privacy.android.app.presentation.myaccount

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import mega.privacy.android.app.presentation.cancelaccountplan.view.INSTRUCTIONS_STEP_WITH_BOLD_TEXT_TEST_TAG
import mega.privacy.android.app.presentation.cancelaccountplan.view.instructionscreens.WEB_CANCELLATION_INSTRUCTIONS_COMPUTER_STEP_WITH_URL_TEST_TAG
import mega.privacy.android.app.presentation.cancelaccountplan.view.instructionscreens.WEB_CANCELLATION_INSTRUCTIONS_MOBILE_STEP_WITH_URL_TEST_TAG
import mega.privacy.android.app.presentation.cancelaccountplan.view.instructionscreens.WebCancellationInstructionsView
import mega.privacy.android.shared.resources.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import mega.privacy.android.app.fromId

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w720dp-h1280dp-xhdpi")
class WebCancellationInstructionsViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that all web cancellation instructions views are showing`() {
        composeTestRule.setContent {
            WebCancellationInstructionsView(onMegaUrlClicked = { })
        }
        composeTestRule.onNodeWithText(fromId(R.string.account_cancellation_instructions_cancellation_web_browser_needed))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(fromId(R.string.account_cancellation_instructions_web_browser_description))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(fromId(R.string.account_cancellation_instructions_on_computer))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(fromId(R.string.account_cancellation_instructions_click_main_menu))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(fromId(R.string.account_cancellation_instructions_on_mobile_device))
            .assertIsDisplayed()
        composeTestRule.onAllNodesWithText(fromId(R.string.account_cancellation_instructions_login_account))
            .assertCountEquals(2)
        composeTestRule.onNodeWithText(fromId(R.string.account_cancellation_instructions_tap_avatar))
            .assertIsDisplayed()
        composeTestRule.onAllNodesWithTag(INSTRUCTIONS_STEP_WITH_BOLD_TEXT_TEST_TAG)
            .assertCountEquals(4)
        composeTestRule.onNodeWithTag(WEB_CANCELLATION_INSTRUCTIONS_COMPUTER_STEP_WITH_URL_TEST_TAG)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(WEB_CANCELLATION_INSTRUCTIONS_MOBILE_STEP_WITH_URL_TEST_TAG)
            .assertIsDisplayed()
    }
}