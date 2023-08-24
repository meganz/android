package test.mega.privacy.android.app.presentation.security.check

import androidx.activity.ComponentActivity
import androidx.compose.material.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableStateFlow
import mega.privacy.android.app.presentation.security.check.PasscodeCheckViewModel
import mega.privacy.android.app.presentation.security.check.PasscodeContainer
import mega.privacy.android.app.presentation.security.check.model.PasscodeCheckState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class PasscodeContainerTest {
    @get:Rule
    var composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val passcodeCheckViewModel = mock<PasscodeCheckViewModel>()

    @Test
    fun `test that content is shown if passcode is not locked`() {
        passcodeCheckViewModel.stub {
            on { state }.thenReturn(MutableStateFlow(PasscodeCheckState.UnLocked))
        }

        val expected = "Expected"
        composeTestRule.setContent {
            PasscodeContainer(
                passcodeUI = {},
                viewModel = passcodeCheckViewModel
            ) {
                Text(expected)
            }
        }

        composeTestRule.onNodeWithText(expected).assertIsDisplayed()
    }

    @Test
    fun `test that content is not shown if passcode is locked`() {
        passcodeCheckViewModel.stub {
            on { state }.thenReturn(MutableStateFlow(PasscodeCheckState.Locked))
        }

        val notExpected = "Not Expected"
        composeTestRule.setContent {
            PasscodeContainer(
                passcodeUI = {},
                viewModel = passcodeCheckViewModel
            ) {
                Text(notExpected)
            }
        }

        composeTestRule.onNodeWithText(notExpected).assertDoesNotExist()
    }

    @Test
    fun `test that passcode dialog is displayed when locked`() {
        passcodeCheckViewModel.stub {
            on { state }.thenReturn(MutableStateFlow(PasscodeCheckState.Locked))
        }

        val expected = "Expected"

        composeTestRule.setContent {
            PasscodeContainer(
                passcodeUI = { Text(expected) },
                viewModel = passcodeCheckViewModel
            ) {}
        }

        composeTestRule.onNodeWithText(expected).assertIsDisplayed()
    }

    @Test
    fun `test that passcode dialog is not displayed when unlocked`() {
        passcodeCheckViewModel.stub {
            on { state }.thenReturn(MutableStateFlow(PasscodeCheckState.UnLocked))
        }

        val notExpected = "Not Expected"

        composeTestRule.setContent {
            PasscodeContainer(
                passcodeUI = { Text(notExpected) },
                viewModel = passcodeCheckViewModel
            ) {}
        }

        composeTestRule.onNodeWithText(notExpected).assertDoesNotExist()
    }

    @Test
    fun `test that content is displayed while loading`() {
        //This behaviour matches current implementation, can potentially be improved
        passcodeCheckViewModel.stub {
            on { state }.thenReturn(MutableStateFlow(PasscodeCheckState.Loading))
        }

        val expected = "Expected"
        composeTestRule.setContent {
            PasscodeContainer(
                passcodeUI = {},
                viewModel = passcodeCheckViewModel
            ) {
                Text(expected)
            }
        }

        composeTestRule.onNodeWithText(expected).assertIsDisplayed()
    }
}