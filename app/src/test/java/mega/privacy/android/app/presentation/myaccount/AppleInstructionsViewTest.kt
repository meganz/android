package mega.privacy.android.app.presentation.myaccount

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import mega.privacy.android.app.presentation.cancelaccountplan.view.INSTRUCTIONS_STEP_WITH_BOLD_TEXT_TEST_TAG
import mega.privacy.android.app.presentation.cancelaccountplan.view.instructionscreens.APPLE_INSTRUCTIONS_DETAILED_INSTRUCTIONS_TEST_TAG
import mega.privacy.android.app.presentation.cancelaccountplan.view.instructionscreens.AppleInstructionsView
import mega.privacy.android.shared.resources.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import mega.privacy.android.app.fromId

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w720dp-h1280dp-xhdpi")
class AppleInstructionsViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that all apple instructions views are showing`() {
        composeTestRule.setContent {
            AppleInstructionsView(onCancelSubsFromOtherDeviceClicked = { })
        }
        composeTestRule.onNodeWithText(fromId(R.string.account_cancellation_instructions_not_managed_by_google))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(fromId(R.string.account_cancellation_instructions_message_apple_description))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(fromId(R.string.account_cancellation_instructions_ios_device))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(fromId(R.string.account_cancellation_instructions_tap_on_name))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(fromId(R.string.account_cancellation_instructions_select_mega_subscription))
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(APPLE_INSTRUCTIONS_DETAILED_INSTRUCTIONS_TEST_TAG)
            .assertIsDisplayed()
        composeTestRule.onAllNodesWithTag(INSTRUCTIONS_STEP_WITH_BOLD_TEXT_TEST_TAG)
            .assertCountEquals(3)
    }
}