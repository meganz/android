package mega.privacy.android.app.presentation.login.createaccount.view

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.android.domain.entity.changepassword.PasswordStrength

@RunWith(AndroidJUnit4::class)
internal class PasswordHintTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun `test that the correct message is displayed for minimum character error`() {
        with(composeRule) {
            setComponent(
                isTitleVisible = true,
                isMinimumCharacterError = true
            )

            val text =
                context.getString(sharedR.string.sign_up_password_min_character_error_message)
            onNodeWithText(text).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the correct message is displayed for a weak password`() {
        with(composeRule) {
            setComponent(
                isTitleVisible = true,
                isMinimumCharacterError = false,
                passwordStrength = PasswordStrength.WEAK
            )

            val text =
                context.getString(sharedR.string.sign_up_password_weak_password_error_message)
            onNodeWithText(text).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the correct message is displayed for a very weak password`() {
        with(composeRule) {
            setComponent(
                isTitleVisible = true,
                isMinimumCharacterError = false,
                passwordStrength = PasswordStrength.VERY_WEAK
            )

            val text =
                context.getString(sharedR.string.sign_up_password_weak_password_error_message)
            onNodeWithText(text).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the correct message is displayed for a medium password`() {
        with(composeRule) {
            setComponent(
                isTitleVisible = true,
                isMinimumCharacterError = false,
                passwordStrength = PasswordStrength.MEDIUM
            )

            val text =
                context.getString(sharedR.string.sign_up_password_medium_password_warning_message)
            onNodeWithText(text).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the correct message is displayed for a good password`() {
        with(composeRule) {
            setComponent(
                isTitleVisible = true,
                isMinimumCharacterError = false,
                passwordStrength = PasswordStrength.GOOD
            )

            val text =
                context.getString(sharedR.string.sign_up_password_min_character_error_message)
            onNodeWithText(text).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the correct message is displayed for a strong password`() {
        with(composeRule) {
            setComponent(
                isTitleVisible = true,
                isMinimumCharacterError = false,
                passwordStrength = PasswordStrength.STRONG
            )

            val text =
                context.getString(sharedR.string.sign_up_password_min_character_error_message)
            onNodeWithText(text).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the correct message is displayed for a short password`() {
        with(composeRule) {
            setComponent(
                isTitleVisible = true,
                isMinimumCharacterError = false,
            )

            val text =
                context.getString(sharedR.string.sign_up_password_min_character_error_message)
            onNodeWithText(text).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the password rules headline is displayed`() {
        with(composeRule) {
            setComponent()

            onNodeWithTag(PASSWORD_HINT_RULES_HEADLINE_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the correct content is displayed for the first password rule if it has been fulfilled`() {
        with(composeRule) {
            setComponent(doesContainMixedCase = true)

            val text =
                context.getString(sharedR.string.sign_up_password_hint_upper_lower_case)
            onNodeWithTag(PASSWORD_HINT_CHECK_ICON_TAG + text).assertIsDisplayed()
            onNodeWithText(text).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the correct content is displayed for the first password rule if it has not been fulfilled`() {
        with(composeRule) {
            setComponent(doesContainMixedCase = false)

            val text =
                context.getString(sharedR.string.sign_up_password_hint_upper_lower_case)
            onNodeWithTag(PASSWORD_HINT_CIRCLE_ICON_TAG + text).assertIsDisplayed()
            onNodeWithText(text).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the correct content is displayed for the second password rule if it has been fulfilled`() {
        with(composeRule) {
            setComponent(doesContainNumberOrSpecialCharacter = true)

            val text =
                context.getString(sharedR.string.sign_up_password_hint_number_or_special_character)
            onNodeWithTag(PASSWORD_HINT_CHECK_ICON_TAG + text).assertIsDisplayed()
            onNodeWithText(text).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the correct content is displayed for the second password rule if it has not been fulfilled`() {
        with(composeRule) {
            setComponent(doesContainNumberOrSpecialCharacter = false)

            val text =
                context.getString(sharedR.string.sign_up_password_hint_number_or_special_character)
            onNodeWithTag(PASSWORD_HINT_CIRCLE_ICON_TAG + text).assertIsDisplayed()
            onNodeWithText(text).assertIsDisplayed()
        }
    }

    private fun ComposeContentTestRule.setComponent(
        modifier: Modifier = Modifier,
        isVisible: Boolean = true,
        doesContainMixedCase: Boolean = false,
        doesContainNumberOrSpecialCharacter: Boolean = false,
        isMinimumCharacterError: Boolean = false,
        passwordStrength: PasswordStrength = PasswordStrength.INVALID,
        isTitleVisible: Boolean = true,
    ) {
        setContent {
            PasswordHint(
                modifier = modifier,
                isVisible = isVisible,
                doesContainMixedCase = doesContainMixedCase,
                doesContainNumberOrSpecialCharacter = doesContainNumberOrSpecialCharacter,
                isMinimumCharacterError = isMinimumCharacterError,
                passwordStrength = passwordStrength,
                isTitleVisible = isTitleVisible
            )
        }
    }
}
