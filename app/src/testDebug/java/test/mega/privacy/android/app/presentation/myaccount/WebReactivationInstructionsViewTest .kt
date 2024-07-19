package test.mega.privacy.android.app.presentation.myaccount

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import mega.privacy.android.app.presentation.cancelaccountplan.view.INSTRUCTIONS_STEP_WITH_BOLD_TEXT_TEST_TAG
import mega.privacy.android.app.presentation.cancelaccountplan.view.instructionscreens.WEB_REACTIVATION_INSTRUCTIONS_COMPUTER_STEP_WITH_URL_TEST_TAG
import mega.privacy.android.app.presentation.cancelaccountplan.view.instructionscreens.WebReactivationInstructionsView
import mega.privacy.android.shared.resources.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import test.mega.privacy.android.app.fromId

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w720dp-h1280dp-xhdpi")
class WebReactivationInstructionsViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that all web cancellation instructions views are showing`() {
        composeTestRule.setContent {
            WebReactivationInstructionsView(onMegaUrlClicked = { })
        }
        composeTestRule.onNodeWithText(fromId(R.string.account_cancellation_instructions_reactivation_web_browser_needed))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(fromId(R.string.account_cancellation_instructions_reactivation_web_browser_description))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(fromId(R.string.account_cancellation_instructions_on_computer))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(fromId(R.string.account_cancellation_instructions_login_account))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(fromId(R.string.account_cancellation_instructions_click_main_menu))
            .assertIsDisplayed()
        composeTestRule.onAllNodesWithTag(INSTRUCTIONS_STEP_WITH_BOLD_TEXT_TEST_TAG)
            .assertCountEquals(3)
        composeTestRule.onNodeWithTag(WEB_REACTIVATION_INSTRUCTIONS_COMPUTER_STEP_WITH_URL_TEST_TAG)
            .assertIsDisplayed()
    }
}