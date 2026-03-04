package mega.privacy.android.feature.payment.presentation.cancelaccountplan.view.instructionscreens

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.feature.payment.fromId
import mega.privacy.android.feature.payment.onNodeWithText
import mega.privacy.android.feature.payment.presentation.cancelaccountplan.view.INSTRUCTIONS_STEP_WITH_BOLD_TEXT_TEST_TAG
import mega.privacy.android.shared.resources.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w720dp-h1280dp-xhdpi")
class WebCancellationInstructionsViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val webInstructionsViewModel = mock<WebInstructionsViewModel> {
        on { domainName } doReturn "mega.nz"
        on { megaUrl } doReturn "https://mega.nz/"
    }

    @Test
    fun `test that all web cancellation instructions views are showing`() {
        composeTestRule.setContent {
            WebCancellationInstructionsView(
                onMegaUrlClicked = { },
                viewModel = webInstructionsViewModel,
            )
        }
        composeTestRule.onNodeWithText(R.string.account_cancellation_instructions_cancellation_web_browser_needed)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(R.string.account_cancellation_instructions_web_browser_description)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(R.string.account_cancellation_instructions_on_computer)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(R.string.account_cancellation_instructions_click_main_menu)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(R.string.account_cancellation_instructions_on_mobile_device)
            .assertIsDisplayed()
        composeTestRule.onAllNodesWithText(fromId(R.string.account_cancellation_instructions_login_account))
            .assertCountEquals(2)
        composeTestRule.onNodeWithText(R.string.account_cancellation_instructions_tap_avatar)
            .assertIsDisplayed()
        composeTestRule.onAllNodesWithTag(INSTRUCTIONS_STEP_WITH_BOLD_TEXT_TEST_TAG)
            .assertCountEquals(4)
        composeTestRule.onNodeWithTag(
            WEB_CANCELLATION_INSTRUCTIONS_COMPUTER_STEP_WITH_URL_TEST_TAG
        )
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(
            WEB_CANCELLATION_INSTRUCTIONS_MOBILE_STEP_WITH_URL_TEST_TAG
        )
            .assertIsDisplayed()
    }
}
