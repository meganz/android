package test.mega.privacy.android.app.presentation.myaccount

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import mega.privacy.android.app.presentation.cancelaccountplan.model.CancellationInstructionsType
import mega.privacy.android.app.presentation.cancelaccountplan.view.APPLE_INSTRUCTIONS_DETAILED_INSTRUCTIONS_TEST_TAG
import mega.privacy.android.app.presentation.cancelaccountplan.view.APPLE_INSTRUCTIONS_SUBTITLE_TEST_TAG
import mega.privacy.android.app.presentation.cancelaccountplan.view.APPLE_INSTRUCTIONS_TEST_TAG
import mega.privacy.android.app.presentation.cancelaccountplan.view.APPLE_INSTRUCTIONS_TITLE_TEST_TAG
import mega.privacy.android.app.presentation.cancelaccountplan.view.APPLE_INSTRUCTIONS_VIEW_TEST_TAG
import mega.privacy.android.app.presentation.cancelaccountplan.view.CancellationInstructionsView
import mega.privacy.android.shared.resources.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import test.mega.privacy.android.app.fromId

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w720dp-h1280dp-xhdpi")
class CancellationInstructionsViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that instructions details view is showing the Apple instructions view`() {
        composeTestRule.setContent {
            CancellationInstructionsView(
                instructionsType = CancellationInstructionsType.AppStore,
                onMegaUrlClicked = { },
                onCancelSubsFromOtherDeviceClicked = { },
                onBackPressed = { },
                isAccountExpired = false
            )
        }
        composeTestRule.onNodeWithTag(APPLE_INSTRUCTIONS_VIEW_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun `test that all apple instructions views are showing`() {
        composeTestRule.setContent {
            CancellationInstructionsView(
                instructionsType = CancellationInstructionsType.AppStore,
                onMegaUrlClicked = { },
                onCancelSubsFromOtherDeviceClicked = { },
                onBackPressed = { },
                isAccountExpired = false
            )
        }
        composeTestRule.onNodeWithTag(APPLE_INSTRUCTIONS_TITLE_TEST_TAG)
            .assertTextEquals(fromId(R.string.account_cancellation_instructions_not_managed_by_google))
        composeTestRule.onNodeWithTag(APPLE_INSTRUCTIONS_SUBTITLE_TEST_TAG)
            .assertTextEquals(fromId(R.string.account_cancellation_instructions_message_apple_description))
        composeTestRule.onNodeWithTag(APPLE_INSTRUCTIONS_DETAILED_INSTRUCTIONS_TEST_TAG)
        composeTestRule.onAllNodesWithTag(APPLE_INSTRUCTIONS_TEST_TAG).assertCountEquals(5)

    }
}