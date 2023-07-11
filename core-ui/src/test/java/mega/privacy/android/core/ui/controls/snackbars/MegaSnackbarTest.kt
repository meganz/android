package mega.privacy.android.core.ui.controls.snackbars

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
internal class MegaSnackbarTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that action message is shown`() {
        val message = "message to test"
        composeTestRule.setContent {
            MegaSnackbar(createSnackbarData(message))
        }
        composeTestRule.onNodeWithText(message).assertExists()
    }

    @Test
    fun `test that action button is shown when action label is set`() {
        val action = "Action"
        composeTestRule.setContent {
            MegaSnackbar(createSnackbarData("message", action))
        }
        composeTestRule.onNodeWithText(action).assertExists()
    }
}