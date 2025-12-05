package mega.privacy.android.app.presentation.cancelaccountplan.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import mega.privacy.android.app.presentation.cancelaccountplan.model.CancellationInstructionsType
import mega.privacy.android.app.presentation.cancelaccountplan.view.instructionscreens.APPLE_INSTRUCTIONS_VIEW_TEST_TAG
import mega.privacy.android.app.presentation.cancelaccountplan.view.instructionscreens.CancellationInstructionsView
import mega.privacy.android.app.presentation.cancelaccountplan.view.instructionscreens.WEB_CANCELLATION_INSTRUCTIONS_VIEW_TEST_TAG
import mega.privacy.android.app.presentation.cancelaccountplan.view.instructionscreens.WEB_REACTIVATION_INSTRUCTIONS_VIEW_TEST_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

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
                isAccountReactivationNeeded = false
            )
        }
        composeTestRule.onNodeWithTag(WEB_CANCELLATION_INSTRUCTIONS_VIEW_TEST_TAG)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(WEB_REACTIVATION_INSTRUCTIONS_VIEW_TEST_TAG)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(APPLE_INSTRUCTIONS_VIEW_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun `test that instructions details view is showing the webclient cancellation instructions view`() {
        composeTestRule.setContent {
            CancellationInstructionsView(
                instructionsType = CancellationInstructionsType.WebClient,
                onMegaUrlClicked = { },
                onCancelSubsFromOtherDeviceClicked = { },
                onBackPressed = { },
                isAccountReactivationNeeded = false
            )
        }
        composeTestRule.onNodeWithTag(APPLE_INSTRUCTIONS_VIEW_TEST_TAG)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(WEB_REACTIVATION_INSTRUCTIONS_VIEW_TEST_TAG)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(WEB_CANCELLATION_INSTRUCTIONS_VIEW_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun `test that instructions details view is showing the webclient reactivation instructions view`() {
        composeTestRule.setContent {
            CancellationInstructionsView(
                instructionsType = CancellationInstructionsType.WebClient,
                onMegaUrlClicked = { },
                onCancelSubsFromOtherDeviceClicked = { },
                onBackPressed = { },
                isAccountReactivationNeeded = true
            )
        }
        composeTestRule.onNodeWithTag(APPLE_INSTRUCTIONS_VIEW_TEST_TAG)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(WEB_CANCELLATION_INSTRUCTIONS_VIEW_TEST_TAG)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(WEB_REACTIVATION_INSTRUCTIONS_VIEW_TEST_TAG)
            .assertIsDisplayed()
    }
}