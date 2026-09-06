package mega.privacy.android.a11ytest.sweep

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.text.input.KeyboardType
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.android.core.ui.components.inputfields.PasswordTextInputField
import mega.android.core.ui.components.inputfields.SearchInputField
import mega.android.core.ui.components.inputfields.TextInputField
import mega.privacy.android.a11ytest.ThemeReporters
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accessibility baseline sweep of the design-system text input family, covering plain,
 * password and search input fields in their supported states.
 */
@RunWith(AndroidJUnit4::class)
class TextInputsAccessibilityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val reporters = ThemeReporters("TextInputsAccessibilityTest")

    @Test
    fun test_that_text_input_fields_pass_accessibility_checks() {
        val sweep = composeTestRule.setSweepContent(reporters) {
            TextInputField(
                modifier = Modifier.fillMaxWidth(),
                keyboardType = KeyboardType.Email,
                text = "jane@mega.nz",
                label = "Email",
            )
            TextInputField(
                modifier = Modifier.fillMaxWidth(),
                keyboardType = KeyboardType.Text,
                label = "First name",
                placeholder = "Enter your first name",
            )
            TextInputField(
                modifier = Modifier.fillMaxWidth(),
                keyboardType = KeyboardType.Text,
                text = "Invalid value",
                label = "Nickname",
                errorText = "This nickname is already taken",
            )
        }

        sweep.checkInEachTheme()
    }

    @Test
    fun test_that_password_input_field_passes_accessibility_checks() {
        val sweep = composeTestRule.setSweepContent(reporters) {
            PasswordTextInputField(
                modifier = Modifier.fillMaxWidth(),
                label = "Password",
                text = "correct horse battery",
            )
        }

        sweep.checkInEachTheme()
    }

    @Test
    fun test_that_search_input_field_passes_accessibility_checks() {
        val sweep = composeTestRule.setSweepContent(reporters) {
            SearchInputField(
                placeHolderText = "Search files",
                text = "",
            )
            SearchInputField(
                placeHolderText = "Search folders",
                text = "holiday",
            )
        }

        sweep.checkInEachTheme()
    }
}
