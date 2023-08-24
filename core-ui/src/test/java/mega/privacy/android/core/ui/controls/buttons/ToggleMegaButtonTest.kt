package mega.privacy.android.core.ui.controls.buttons

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.core.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class ToggleMegaButtonTest {

    @get:Rule
    var composeTestRule = createComposeRule()

    @Test
    fun `test that ToggleMegaButton is shown when enabled`() {
        composeTestRule.setContent {
            ToggleMegaButton(
                modifier = Modifier,
                checked = true,
                enabled = true,
                title = "Mic",
                enabledIcon = R.drawable.ic_universal_mic_on,
                disabledIcon = R.drawable.ic_universal_mic_off,
                onCheckedChange = {},
            )
        }
        composeTestRule.onNodeWithTag("toggle_mega_button:toggle", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag("toggle_mega_button:icon", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag("toggle_mega_button:text", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `test that ToggleMegaButton is shown when disabled`() {
        composeTestRule.setContent {
            ToggleMegaButton(
                modifier = Modifier,
                checked = true,
                enabled = true,
                title = "Mic",
                enabledIcon = R.drawable.ic_universal_mic_on,
                disabledIcon = R.drawable.ic_universal_mic_off,
                onCheckedChange = {},
            )
        }
        composeTestRule.onNodeWithTag("toggle_mega_button:toggle", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag("toggle_mega_button:icon", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag("toggle_mega_button:text", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `test that onCheckedChange event is fired when ToggleMegaButton is clicked`() {
        val mock = mock<(Boolean) -> Unit>()
        composeTestRule.setContent {
            ToggleMegaButton(
                modifier = Modifier,
                checked = false,
                enabled = true,
                title = "Mic",
                enabledIcon = R.drawable.ic_universal_mic_on,
                disabledIcon = R.drawable.ic_universal_mic_off,
                onCheckedChange = mock
            )
        }

        composeTestRule.onNodeWithTag("toggle_mega_button:toggle").performClick()
        verify(mock).invoke(true)
    }

    @Test
    fun `test that ToggleMegaButton is disabled when enabled is set to false`() {
        composeTestRule.setContent {
            ToggleMegaButton(
                modifier = Modifier,
                checked = true,
                enabled = false,
                title = "Mic",
                enabledIcon = R.drawable.ic_universal_mic_on,
                disabledIcon = R.drawable.ic_universal_mic_off,
                onCheckedChange = {},
            )
        }
        composeTestRule.onNodeWithTag("toggle_mega_button:toggle").assertIsNotEnabled()
    }

    @Test
    fun `test that ToggleMegaButton is enabled when enabled is set to true`() {
        composeTestRule.setContent {
            ToggleMegaButton(
                modifier = Modifier,
                checked = true,
                enabled = true,
                title = "Mic",
                enabledIcon = R.drawable.ic_universal_mic_on,
                disabledIcon = R.drawable.ic_universal_mic_off,
                onCheckedChange = {},
            )
        }
        composeTestRule.onNodeWithTag("toggle_mega_button:toggle").assertIsEnabled()
    }

    @Test
    fun `test that ToggleMegaButton title matches the text field`() {
        val title = "Mic"

        composeTestRule.setContent {
            ToggleMegaButton(
                modifier = Modifier,
                checked = true,
                enabled = true,
                title = title,
                enabledIcon = R.drawable.ic_universal_mic_on,
                disabledIcon = R.drawable.ic_universal_mic_off,
                onCheckedChange = {},
            )
        }

        composeTestRule.onNodeWithTag("toggle_mega_button:text").assert(hasText(title))
    }
}
