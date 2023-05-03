package test.mega.privacy.android.app.presentation.twofactorauthentication.view

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.twofactorauthentication.view.FIFTH_PIN_TEST_TAG
import mega.privacy.android.app.presentation.twofactorauthentication.view.FIRST_PIN_TEST_TAG
import mega.privacy.android.app.presentation.twofactorauthentication.view.FOURTH_PIN_TEST_TAG
import mega.privacy.android.app.presentation.twofactorauthentication.view.SECOND_PIN_TEST_TAG
import mega.privacy.android.app.presentation.twofactorauthentication.view.SIXTH_PIN_TEST_TAG
import mega.privacy.android.app.presentation.twofactorauthentication.view.THIRD_PIN_TEST_TAG
import mega.privacy.android.app.presentation.twofactorauthentication.view.TWO_FACTOR_AUTHENTICATION_TEST_TAG
import mega.privacy.android.app.presentation.twofactorauthentication.view.TwoFactorAuthenticationField
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TwoFactorAuthenticationFieldTest {

    @get:Rule
    var composeRule = createComposeRule()

    private fun setupRule(
        twoFAPin: List<String> = listOf("", "", "", "", "", ""),
    ) {
        composeRule.setContent {
            TwoFactorAuthenticationField(
                twoFAPin = twoFAPin,
                on2FAPinChanged = { _, _ -> },
                on2FAChanged = {},
                isError = false,
                shouldRequestFocus = false,
            )
        }
    }

    @Test
    fun `test that 2FA field exist`() {
        setupRule()
        composeRule.onNodeWithTag(TWO_FACTOR_AUTHENTICATION_TEST_TAG).assertExists()
    }

    @Test
    fun `test that 2FA pins exists`() {
        setupRule()
        composeRule.onNodeWithTag(FIRST_PIN_TEST_TAG).assertExists()
        composeRule.onNodeWithTag(SECOND_PIN_TEST_TAG).assertExists()
        composeRule.onNodeWithTag(THIRD_PIN_TEST_TAG).assertExists()
        composeRule.onNodeWithTag(FOURTH_PIN_TEST_TAG).assertExists()
        composeRule.onNodeWithTag(FIFTH_PIN_TEST_TAG).assertExists()
        composeRule.onNodeWithTag(SIXTH_PIN_TEST_TAG).assertExists()
    }

    @Test
    fun `test that 2FA typed is shown`() {
        val firstPin = "1"
        val secondPin = "2"
        val thirdPin = "3"
        val fourthPin = "4"
        val fifthPin = "5"
        val sixthPin = "6"
        setupRule(twoFAPin = listOf(firstPin, secondPin, thirdPin, fourthPin, fifthPin, sixthPin))
        composeRule.onNodeWithText(firstPin).assertExists()
        composeRule.onNodeWithText(secondPin).assertExists()
        composeRule.onNodeWithText(thirdPin).assertExists()
        composeRule.onNodeWithText(fourthPin).assertExists()
        composeRule.onNodeWithText(fifthPin).assertExists()
        composeRule.onNodeWithText(sixthPin).assertExists()
    }
}