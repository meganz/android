package test.mega.privacy.android.app.presentation.settings.startscreen.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.startscreen.view.StartScreenOptionView
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class StartScreenOptionViewTest {

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that text is displayed`() {
        val expected = "Home"
        composeRule.setContent {
            StartScreenOptionView(
                icon = R.drawable.ic_homepage,
                text = expected,
                isSelected = false,
                onClick = {}
            )
        }

        composeRule.onNodeWithText(expected).assertIsDisplayed()
    }

    @Test
    fun `test that icon is displayed`() {
        val expectedResId = R.drawable.ic_homepage
        composeRule.setContent {
            StartScreenOptionView(
                icon = expectedResId,
                text = "expected",
                isSelected = false,
                onClick = {}
            )
        }

        composeRule.onNodeWithTag(expectedResId.toString(), useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that checkmark is displayed if selected`() {
        composeRule.setContent {
            StartScreenOptionView(
                icon = R.drawable.ic_homepage,
                text = "expected",
                isSelected = true,
                onClick = {}
            )
        }

        composeRule.onNodeWithTag(R.drawable.ic_check.toString(), useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that checkmark is not displayed if not selected`() {
        composeRule.setContent {
            StartScreenOptionView(
                icon = R.drawable.ic_homepage,
                text = "expected",
                isSelected = false,
                onClick = {}
            )
        }

        composeRule.onNodeWithTag(R.drawable.ic_check.toString()).assertDoesNotExist()
    }

    @Test
    fun `test that on click is called when control is clicked`() {
        val onClick = mock<() -> Unit>()
        composeRule.setContent {
            StartScreenOptionView(
                icon = R.drawable.ic_homepage,
                text = "expected",
                isSelected = false,
                onClick = onClick
            )
        }

        composeRule.onRoot().performClick()

        verify(onClick).invoke()
    }
}